package com.oism.application.inventory;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import com.oism.domain.model.Stocktake;
import com.oism.domain.model.StocktakeItem;
import com.oism.domain.model.StocktakeStatus;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import com.oism.domain.repository.StocktakeItemRepository;
import com.oism.domain.repository.StocktakeRepository;
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
 * Quan ly phien kiem ke kho (FR-INV-04):
 * Mo phien -> Nhap so dem thuc te -> Duyet phien -> Tu dong tao but toan dieu chinh ADJUST cho cac lech kho.
 */
@Service
public class StocktakeService {

    private static final String REF_TYPE_STOCKTAKE = "STOCKTAKE";

    private final StocktakeRepository stocktakeRepository;
    private final StocktakeItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryLedgerService ledgerService;
    private final SecureRandom random = new SecureRandom();

    public StocktakeService(StocktakeRepository stocktakeRepository,
                            StocktakeItemRepository itemRepository,
                            BranchRepository branchRepository,
                            SkuRepository skuRepository,
                            StockBalanceRepository stockBalanceRepository,
                            InventoryLedgerService ledgerService) {
        this.stocktakeRepository = stocktakeRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
        this.stockBalanceRepository = stockBalanceRepository;
        this.ledgerService = ledgerService;
    }

    public record CountInput(UUID skuId, int countedQuantity) {
    }

    public record StocktakeItemView(
            UUID skuId,
            String skuCode,
            int systemQuantity,
            Integer countedQuantity,
            Integer difference) {
    }

    public record StocktakeView(
            UUID id,
            String code,
            UUID branchId,
            String branchName,
            StocktakeStatus status,
            UUID approvedBy,
            Instant createdAt,
            List<StocktakeItemView> items) {
    }

    @Transactional
    public StocktakeView create(UUID tenantId, UUID userId, UUID branchId, List<UUID> skuIds) {
        Branch branch = branchRepository.findByIdAndTenantId(branchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40001, "Chi nhánh không hợp lệ"));

        List<Sku> targetSkus;
        if (skuIds == null || skuIds.isEmpty()) {
            targetSkus = skuRepository.findByTenantIdAndStatus(tenantId, CatalogStatus.ACTIVE);
        } else {
            targetSkus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds);
        }

        if (targetSkus.isEmpty()) {
            throw new BusinessException(ErrorCode.IV40001, "Không có SKU nào để kiểm kê");
        }

        Stocktake stocktake = stocktakeRepository.save(new Stocktake(tenantId, branchId, newCode(tenantId)));

        List<StocktakeItem> items = new ArrayList<>();
        for (Sku sku : targetSkus) {
            stockBalanceRepository.insertIfAbsent(tenantId, branchId, sku.getId());
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, sku.getId())
                    .orElseThrow(() -> new IllegalStateException("Không lấy được tồn"));

            items.add(new StocktakeItem(tenantId, stocktake.getId(), sku.getId(), balance.getOnHand()));
        }
        itemRepository.saveAll(items);

        return view(tenantId, stocktake, branch.getName());
    }

    @Transactional
    public StocktakeView recordCounts(UUID tenantId, UUID userId, UUID stocktakeId, List<CountInput> counts) {
        Stocktake stocktake = stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiên kiểm kê"));

        stocktake.assertOpen();

        List<StocktakeItem> items = itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId);
        Map<UUID, StocktakeItem> itemMap = items.stream().collect(Collectors.toMap(StocktakeItem::getSkuId, i -> i));

        for (CountInput c : counts) {
            if (c.countedQuantity() < 0) {
                throw new BusinessException(ErrorCode.IV40001, "Số lượng đếm không được âm");
            }
            StocktakeItem item = itemMap.get(c.skuId());
            if (item != null) {
                item.updateCountedQty(c.countedQuantity());
                itemRepository.save(item);
            }
        }

        return get(tenantId, stocktakeId);
    }

    /** Duyet phien kiem ke: voi moi dong co difference != 0, tu dong tao but toan ADJUST */
    @Transactional
    public StocktakeView approve(UUID tenantId, UUID userId, UUID stocktakeId) {
        Stocktake stocktake = stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiên kiểm kê"));

        stocktake.markApproved(userId);
        stocktakeRepository.save(stocktake);

        List<StocktakeItem> items = itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId);
        for (StocktakeItem item : items) {
            Integer diff = item.getDifference();
            if (diff == null || diff == 0) {
                continue;
            }

            Sku sku = skuRepository.findByIdAndTenantId(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));

            BigDecimal unitCost = sku.getAvgCost();

            if (diff > 0) {
                // Thua so voi he thong -> Nhap dieu chinh tang
                ledgerService.post(new LedgerEntry(
                        tenantId, stocktake.getBranchId(), item.getSkuId(), StockDirection.IN,
                        StockReason.ADJUST, diff, unitCost, REF_TYPE_STOCKTAKE, stocktake.getId(), userId
                ));
            } else {
                // Thieu so voi he thong -> Xuat dieu chinh giam
                ledgerService.post(new LedgerEntry(
                        tenantId, stocktake.getBranchId(), item.getSkuId(), StockDirection.OUT,
                        StockReason.ADJUST, Math.abs(diff), unitCost, REF_TYPE_STOCKTAKE, stocktake.getId(), userId
                ));
            }
        }

        return get(tenantId, stocktakeId);
    }

    @Transactional(readOnly = true)
    public StocktakeView get(UUID tenantId, UUID id) {
        Stocktake stocktake = stocktakeRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.IV40401, "Không tìm thấy phiên kiểm kê"));

        String branchName = branchRepository.findByIdAndTenantId(stocktake.getBranchId(), tenantId)
                .map(Branch::getName).orElse(stocktake.getBranchId().toString());

        return view(tenantId, stocktake, branchName);
    }

    @Transactional(readOnly = true)
    public PageResult<StocktakeView> list(UUID tenantId, StocktakeStatus status, UUID branchId, Integer page, Integer size) {
        Specification<Stocktake> spec = (root, query, cb) -> {
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

        Page<Stocktake> result = stocktakeRepository.findAll(spec, Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<UUID, String> branchNames = branchRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        return PageResult.of(result, s -> view(tenantId, s, branchNames.getOrDefault(s.getBranchId(), s.getBranchId().toString())));
    }

    private StocktakeView view(UUID tenantId, Stocktake s, String branchName) {
        List<StocktakeItem> items = itemRepository.findByTenantIdAndStocktakeId(tenantId, s.getId());
        Map<UUID, String> skuCodes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(StocktakeItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));

        List<StocktakeItemView> itemViews = items.stream()
                .map(i -> new StocktakeItemView(
                        i.getSkuId(),
                        skuCodes.getOrDefault(i.getSkuId(), i.getSkuId().toString()),
                        i.getSystemQty(),
                        i.getCountedQty(),
                        i.getDifference()))
                .toList();

        return new StocktakeView(s.getId(), s.getCode(), s.getBranchId(), branchName, s.getStatus(), s.getApprovedBy(),
                s.getCreatedAt(), itemViews);
    }

    private String newCode(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 20; i++) {
            String code = "KK-" + date + "-" + String.format("%04d", random.nextInt(10_000));
            if (!stocktakeRepository.existsByTenantIdAndCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã phiếu kiểm kê");
    }
}
