package com.oism.application.inventory;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.PurchaseReceipt;
import com.oism.domain.model.PurchaseReceiptItem;
import com.oism.domain.model.ReceiptStatus;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.PurchaseReceiptItemRepository;
import com.oism.domain.repository.PurchaseReceiptRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import com.oism.domain.repository.SupplierRepository;
import com.oism.domain.service.CostCalculator;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-INV-02 + FR-COST-01: phieu nhap kho Nhap -> Da xac nhan. Chi khi xac nhan moi ghi so cai IN,
 * tang on_hand va tinh lai gia von binh quan gia quyen cua tung SKU.
 */
@Service
public class PurchaseReceiptService {

    private static final String REF_TYPE = "PURCHASE_RECEIPT";

    private final PurchaseReceiptRepository receiptRepository;
    private final PurchaseReceiptItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final SupplierRepository supplierRepository;
    private final SkuRepository skuRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryLedgerService ledgerService;
    private final SecureRandom random = new SecureRandom();

    public PurchaseReceiptService(PurchaseReceiptRepository receiptRepository,
                                  PurchaseReceiptItemRepository itemRepository,
                                  BranchRepository branchRepository,
                                  SupplierRepository supplierRepository,
                                  SkuRepository skuRepository,
                                  StockBalanceRepository stockBalanceRepository,
                                  InventoryLedgerService ledgerService) {
        this.receiptRepository = receiptRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.supplierRepository = supplierRepository;
        this.skuRepository = skuRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public ReceiptView create(UUID tenantId, UUID userId, UUID branchId, UUID supplierId, List<ReceiptLine> lines) {
        assertBranchAndSupplier(tenantId, branchId, supplierId);
        assertLines(tenantId, lines);
        PurchaseReceipt receipt = receiptRepository.save(
                new PurchaseReceipt(tenantId, branchId, supplierId, newCode(tenantId), userId));
        saveItems(tenantId, receipt.getId(), lines);
        return view(tenantId, receipt);
    }

    /** Chi sua duoc khi con o trang thai Nhap; phieu da xac nhan -> IV40902. */
    @Transactional
    public ReceiptView update(UUID tenantId, UUID id, UUID branchId, UUID supplierId, List<ReceiptLine> lines) {
        PurchaseReceipt receipt = find(tenantId, id);
        receipt.assertEditable();
        assertBranchAndSupplier(tenantId, branchId, supplierId);
        assertLines(tenantId, lines);
        receipt.update(branchId, supplierId);
        receiptRepository.save(receipt);
        itemRepository.deleteByTenantIdAndReceiptId(tenantId, id);
        itemRepository.flush();
        saveItems(tenantId, id, lines);
        return view(tenantId, receipt);
    }

    /**
     * Xac nhan phieu: voi moi SKU (theo thu tu tang dan cua sku_id de tranh deadlock) khoa dong SKU,
     * tinh gia von moi tu ton cu tren toan tenant, ghi so cai IN, cap nhat avg_cost. Tat ca trong mot giao dich.
     */
    @Transactional
    public ReceiptView confirm(UUID tenantId, UUID userId, UUID id) {
        PurchaseReceipt receipt = find(tenantId, id);
        receipt.assertEditable();
        List<PurchaseReceiptItem> items = itemRepository.findByTenantIdAndReceiptId(tenantId, id).stream()
                .sorted(Comparator.comparing(PurchaseReceiptItem::getSkuId))
                .toList();
        if (items.isEmpty()) {
            throw new BusinessException(ErrorCode.IV40001, "Phiếu nhập chưa có dòng hàng nào");
        }
        Branch branch = branchRepository.findByIdAndTenantId(receipt.getBranchId(), tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40001, "Chi nhánh không hợp lệ"));

        for (PurchaseReceiptItem item : items) {
            Sku sku = skuRepository.findForUpdate(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));
            long oldOnHand = stockBalanceRepository.sumOnHand(tenantId, sku.getId());
            BigDecimal newCost = CostCalculator.weightedAverage(
                    oldOnHand, sku.getAvgCost(), item.getQuantity(), item.getUnitPrice());

            ledgerService.post(new LedgerEntry(tenantId, branch.getId(), sku.getId(), StockDirection.IN,
                    StockReason.PURCHASE, item.getQuantity(), item.getUnitPrice(), REF_TYPE, receipt.getId(), userId));

            sku.setAvgCost(newCost);
            skuRepository.save(sku);
        }

        receipt.markConfirmed(userId);
        receiptRepository.save(receipt);
        return view(tenantId, receipt);
    }

    @Transactional(readOnly = true)
    public ReceiptView get(UUID tenantId, UUID id) {
        return view(tenantId, find(tenantId, id));
    }

    @Transactional(readOnly = true)
    public PageResult<ReceiptView> list(UUID tenantId, ReceiptStatus status, UUID branchId, Integer page, Integer size) {
        Specification<PurchaseReceipt> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (branchId != null) {
                ps.add(cb.equal(root.get("branchId"), branchId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<PurchaseReceipt> result = receiptRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result, r -> view(tenantId, r));
    }

    private ReceiptView view(UUID tenantId, PurchaseReceipt r) {
        List<PurchaseReceiptItem> items = itemRepository.findByTenantIdAndReceiptId(tenantId, r.getId());
        Map<UUID, String> codes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(PurchaseReceiptItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));
        List<ReceiptView.Item> lines = items.stream()
                .map(i -> new ReceiptView.Item(i.getSkuId(), codes.get(i.getSkuId()), i.getQuantity(),
                        i.getUnitPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        BigDecimal total = lines.stream().map(ReceiptView.Item::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReceiptView(r.getId(), r.getCode(), r.getBranchId(), r.getSupplierId(), r.getStatus(),
                r.getConfirmedAt(), r.getCreatedAt(), total, lines);
    }

    private PurchaseReceipt find(UUID tenantId, UUID id) {
        return receiptRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiếu nhập"));
    }

    private void assertBranchAndSupplier(UUID tenantId, UUID branchId, UUID supplierId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40001, "Chi nhánh không hợp lệ"));
        if (supplierId != null && supplierRepository.findByIdAndTenantId(supplierId, tenantId).isEmpty()) {
            throw new BusinessException(ErrorCode.IV40001, "Nhà cung cấp không hợp lệ");
        }
    }

    private void assertLines(UUID tenantId, List<ReceiptLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.IV40001, "Phiếu nhập cần ít nhất 1 dòng hàng");
        }
        for (ReceiptLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BusinessException(ErrorCode.IV40001, "Số lượng phải lớn hơn 0");
            }
            if (line.unitPrice() == null || line.unitPrice().signum() < 0) {
                throw new BusinessException(ErrorCode.IV40001, "Đơn giá không được âm");
            }
        }
        List<UUID> skuIds = lines.stream().map(ReceiptLine::skuId).distinct().toList();
        if (skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.PR40401, "Có SKU không tồn tại");
        }
    }

    private void saveItems(UUID tenantId, UUID receiptId, List<ReceiptLine> lines) {
        itemRepository.saveAll(lines.stream()
                .map(l -> new PurchaseReceiptItem(tenantId, receiptId, l.skuId(), l.quantity(), l.unitPrice()))
                .toList());
    }

    private String newCode(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 20; i++) {
            String code = "PN-" + date + "-" + String.format("%04d", random.nextInt(10_000));
            if (!receiptRepository.existsByTenantIdAndCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã phiếu nhập");
    }
}
