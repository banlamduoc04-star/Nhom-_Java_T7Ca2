package com.oism.application.inventory;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import com.oism.domain.model.StockTransfer;
import com.oism.domain.model.StockTransferItem;
import com.oism.domain.model.TransferStatus;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockTransferItemRepository;
import com.oism.domain.repository.StockTransferRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quan ly chuyen kho 2 buoc giua 2 chi nhanh (FR-INV-03):
 * Tao Draft -> Xuat kho (In Transit, ghi OUT so cai nguon) -> Nhan kho (Received, ghi IN so cai dich).
 */
@Service
public class StockTransferService {

    private static final String REF_TYPE_TRANSFER = "TRANSFER";

    private final StockTransferRepository transferRepository;
    private final StockTransferItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final InventoryLedgerService ledgerService;
    private final SecureRandom random = new SecureRandom();

    public StockTransferService(StockTransferRepository transferRepository,
                                StockTransferItemRepository itemRepository,
                                BranchRepository branchRepository,
                                SkuRepository skuRepository,
                                InventoryLedgerService ledgerService) {
        this.transferRepository = transferRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
        this.ledgerService = ledgerService;
    }

    public record TransferLine(UUID skuId, int quantity) {
    }

    public record TransferItemView(UUID skuId, String skuCode, int quantity) {
    }

    public record TransferView(
            UUID id,
            String code,
            UUID fromBranchId,
            String fromBranchName,
            UUID toBranchId,
            String toBranchName,
            TransferStatus status,
            Instant createdAt,
            List<TransferItemView> items) {
    }

    @Transactional
    public TransferView create(UUID tenantId, UUID userId, UUID fromBranchId, UUID toBranchId, List<TransferLine> lines) {
        if (fromBranchId.equals(toBranchId)) {
            throw new BusinessException(ErrorCode.IV40001, "Chi nhánh xuất và nhận không được trùng nhau");
        }
        Branch fromBranch = branchRepository.findByIdAndTenantId(fromBranchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40001, "Chi nhánh xuất không hợp lệ"));
        Branch toBranch = branchRepository.findByIdAndTenantId(toBranchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40001, "Chi nhánh nhận không hợp lệ"));

        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.IV40001, "Phiếu chuyển kho cần ít nhất 1 dòng hàng");
        }

        List<UUID> skuIds = lines.stream().map(TransferLine::skuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        if (skus.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.PR40401, "Có SKU không tồn tại");
        }

        for (TransferLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BusinessException(ErrorCode.IV40001, "Số lượng chuyển phải lớn hơn 0");
            }
            Sku sku = skus.get(line.skuId());
            if (sku.getStatus() != CatalogStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.IV40001, "SKU " + sku.getCode() + " đã ngừng hoạt động");
            }
        }

        StockTransfer transfer = transferRepository.save(new StockTransfer(tenantId, fromBranchId, toBranchId, newCode(tenantId)));
        itemRepository.saveAll(lines.stream()
                .map(l -> new StockTransferItem(tenantId, transfer.getId(), l.skuId(), l.quantity()))
                .toList());

        return view(tenantId, transfer, fromBranch.getName(), toBranch.getName());
    }

    /** Xuat kho (DRAFT -> IN_TRANSIT): ghi but toan OUT_TRANSFER tai chi nhanh xuat */
    @Transactional
    public TransferView dispatch(UUID tenantId, UUID userId, UUID transferId) {
        StockTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiếu chuyển kho"));

        transfer.markInTransit();
        transferRepository.save(transfer);

        List<StockTransferItem> items = itemRepository.findByTenantIdAndTransferId(tenantId, transferId);
        for (StockTransferItem item : items) {
            Sku sku = skuRepository.findByIdAndTenantId(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));

            BigDecimal unitCost = sku.getAvgCost();
            ledgerService.post(new LedgerEntry(
                    tenantId, transfer.getFromBranchId(), item.getSkuId(), StockDirection.OUT,
                    StockReason.TRANSFER_OUT, item.getQuantity(), unitCost, REF_TYPE_TRANSFER, transfer.getId(), userId
            ));
        }

        return get(tenantId, transferId);
    }

    /** Nhan kho (IN_TRANSIT -> RECEIVED): ghi but toan IN_TRANSFER tai chi nhanh nhan */
    @Transactional
    public TransferView receive(UUID tenantId, UUID userId, UUID transferId) {
        StockTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiếu chuyển kho"));

        transfer.markReceived();
        transferRepository.save(transfer);

        List<StockTransferItem> items = itemRepository.findByTenantIdAndTransferId(tenantId, transferId);
        for (StockTransferItem item : items) {
            Sku sku = skuRepository.findByIdAndTenantId(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));

            BigDecimal unitCost = sku.getAvgCost();
            ledgerService.post(new LedgerEntry(
                    tenantId, transfer.getToBranchId(), item.getSkuId(), StockDirection.IN,
                    StockReason.TRANSFER_IN, item.getQuantity(), unitCost, REF_TYPE_TRANSFER, transfer.getId(), userId
            ));
        }

        return get(tenantId, transferId);
    }

    /** Huy phieu chuyen (chi duoc huy khi o trang thai DRAFT) */
    @Transactional
    public TransferView cancel(UUID tenantId, UUID userId, UUID transferId) {
        StockTransfer transfer = transferRepository.findByIdAndTenantId(transferId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiếu chuyển kho"));

        transfer.markCancelled();
        transferRepository.save(transfer);

        return get(tenantId, transferId);
    }

    @Transactional(readOnly = true)
    public TransferView get(UUID tenantId, UUID id) {
        StockTransfer transfer = transferRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiếu chuyển kho"));

        String fromName = branchRepository.findByIdAndTenantId(transfer.getFromBranchId(), tenantId)
                .map(Branch::getName).orElse(transfer.getFromBranchId().toString());
        String toName = branchRepository.findByIdAndTenantId(transfer.getToBranchId(), tenantId)
                .map(Branch::getName).orElse(transfer.getToBranchId().toString());

        return view(tenantId, transfer, fromName, toName);
    }

    @Transactional(readOnly = true)
    public PageResult<TransferView> list(UUID tenantId, TransferStatus status, UUID fromBranchId, UUID toBranchId, Integer page, Integer size) {
        Specification<StockTransfer> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (fromBranchId != null) {
                ps.add(cb.equal(root.get("fromBranchId"), fromBranchId));
            }
            if (toBranchId != null) {
                ps.add(cb.equal(root.get("toBranchId"), toBranchId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        Page<StockTransfer> result = transferRepository.findAll(spec, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<UUID, String> branchNames = branchRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        return PageResult.of(result, t -> view(tenantId, t,
                branchNames.getOrDefault(t.getFromBranchId(), t.getFromBranchId().toString()),
                branchNames.getOrDefault(t.getToBranchId(), t.getToBranchId().toString())));
    }

    private TransferView view(UUID tenantId, StockTransfer t, String fromName, String toName) {
        List<StockTransferItem> items = itemRepository.findByTenantIdAndTransferId(tenantId, t.getId());
        Map<UUID, String> skuCodes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(StockTransferItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));

        List<TransferItemView> itemViews = items.stream()
                .map(i -> new TransferItemView(i.getSkuId(), skuCodes.getOrDefault(i.getSkuId(), i.getSkuId().toString()), i.getQuantity()))
                .toList();

        return new TransferView(t.getId(), t.getCode(), t.getFromBranchId(), fromName, t.getToBranchId(), toName,
                t.getStatus(), t.getCreatedAt(), itemViews);
    }

    private String newCode(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 20; i++) {
            String code = "CK-" + date + "-" + String.format("%04d", random.nextInt(10_000));
            if (!transferRepository.existsByTenantIdAndCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã phiếu chuyển kho");
    }
}
