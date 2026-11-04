package com.oism.application.inventory;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.model.InventoryTransaction;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.InventoryTransactionRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Truy van ton kho (GET /stock) va so cai (GET /inventory-transactions). Chi doc. */
@Service
public class StockService {

    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SkuRepository skuRepository;

    public StockService(StockBalanceRepository stockBalanceRepository,
                        InventoryTransactionRepository inventoryTransactionRepository,
                        SkuRepository skuRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.skuRepository = skuRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<StockView> stock(UUID tenantId, UUID branchId, UUID skuId, Integer page, Integer size) {
        Specification<StockBalance> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (branchId != null) {
                ps.add(cb.equal(root.get("branchId"), branchId));
            }
            if (skuId != null) {
                ps.add(cb.equal(root.get("skuId"), skuId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<StockBalance> result = stockBalanceRepository.findAll(spec,
                Paging.of(page, size, Sort.by("branchId", "skuId")));
        Map<UUID, String> codes = skuCodes(tenantId, result.getContent().stream().map(StockBalance::getSkuId).toList());
        return PageResult.of(result, b -> new StockView(b.getBranchId(), b.getSkuId(), codes.get(b.getSkuId()),
                b.getOnHand(), b.getReserved(), b.getAvailable()));
    }

    @Transactional(readOnly = true)
    public PageResult<TransactionView> transactions(UUID tenantId, UUID branchId, UUID skuId,
                                                    Instant from, Instant to, Integer page, Integer size) {
        Specification<InventoryTransaction> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (branchId != null) {
                ps.add(cb.equal(root.get("branchId"), branchId));
            }
            if (skuId != null) {
                ps.add(cb.equal(root.get("skuId"), skuId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<InventoryTransaction> result = inventoryTransactionRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, String> codes = skuCodes(tenantId,
                result.getContent().stream().map(InventoryTransaction::getSkuId).toList());
        return PageResult.of(result, t -> new TransactionView(t.getId(), t.getBranchId(), t.getSkuId(),
                codes.get(t.getSkuId()), t.getDirection(), t.getReason(), t.getQuantity(), t.getBalanceAfter(),
                t.getUnitCost(), t.getRefType(), t.getRefId(), t.getCreatedBy(), t.getCreatedAt()));
    }

    private Map<UUID, String> skuCodes(UUID tenantId, Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        return skuRepository.findByTenantIdAndIdIn(tenantId, skuIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode, (a, b) -> a, java.util.HashMap::new));
    }
}
