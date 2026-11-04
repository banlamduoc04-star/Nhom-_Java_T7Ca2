package com.oism.application.report;

import com.oism.domain.model.Branch;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.Product;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bao cao ton kho va toc do ban (FR-REP-02, JC-29):
 * 1. Gia tri ton = on_hand * gia von (theo SKU / chi nhanh)
 * 2. Top 10 ban chay theo so luong va doanh thu
 * 3. Hang ton cham: con ton nhung khong ban trong N ngay (mac dinh 30 ngay)
 */
@Service
public class StockReportService {

    private final StockBalanceRepository stockBalanceRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public StockReportService(StockBalanceRepository stockBalanceRepository,
                              SkuRepository skuRepository,
                              ProductRepository productRepository,
                              BranchRepository branchRepository,
                              SalesOrderRepository salesOrderRepository,
                              OrderItemRepository orderItemRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public StockReportViews.ValuationSummary getValuation(UUID tenantId, UUID branchId) {
        List<StockBalance> balances = stockBalanceRepository.findAll((root, query, cb) -> {
            var p = cb.equal(root.get("tenantId"), tenantId);
            if (branchId != null) {
                return cb.and(p, cb.equal(root.get("branchId"), branchId));
            }
            return p;
        });

        List<UUID> branchIds = balances.stream().map(StockBalance::getBranchId).distinct().toList();
        Map<UUID, String> branchNames = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        List<UUID> skuIds = balances.stream().map(StockBalance::getSkuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        List<UUID> prodIds = skus.values().stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findAllById(prodIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        BigDecimal totalValuation = BigDecimal.ZERO;
        int totalOnHand = 0;
        List<StockReportViews.ValuationItem> items = new ArrayList<>();

        for (StockBalance b : balances) {
            Sku sku = skus.get(b.getSkuId());
            if (sku == null) {
                continue;
            }
            String pName = prodNames.getOrDefault(sku.getProductId(), sku.getCode());
            BigDecimal avgCost = sku.getAvgCost() != null ? sku.getAvgCost() : BigDecimal.ZERO;
            BigDecimal lineValue = avgCost.multiply(BigDecimal.valueOf(b.getOnHand()));

            totalValuation = totalValuation.add(lineValue);
            totalOnHand += b.getOnHand();

            items.add(new StockReportViews.ValuationItem(
                    b.getBranchId(),
                    branchNames.getOrDefault(b.getBranchId(), "N/A"),
                    sku.getId(),
                    sku.getCode(),
                    pName,
                    b.getOnHand(),
                    avgCost,
                    lineValue
            ));
        }

        items.sort(Comparator.comparing(StockReportViews.ValuationItem::totalValue).reversed());
        return new StockReportViews.ValuationSummary(totalValuation, totalOnHand, items.size(), items);
    }

    @Transactional(readOnly = true)
    public List<StockReportViews.TopSellingItem> getTopSelling(UUID tenantId, int days, int limit) {
        Instant since = Instant.now().minus(Math.max(days, 1), ChronoUnit.DAYS);

        List<SalesOrder> orders = salesOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("status").in(OrderStatus.CONFIRMED, OrderStatus.COMPLETED),
                cb.greaterThanOrEqualTo(root.get("createdAt"), since)
        ));

        if (orders.isEmpty()) {
            return List.of();
        }

        List<UUID> orderIds = orders.stream().map(SalesOrder::getId).toList();
        List<OrderItem> orderItems = orderItemRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);

        Map<UUID, Integer> skuQuantities = new HashMap<>();
        Map<UUID, BigDecimal> skuRevenues = new HashMap<>();

        for (OrderItem it : orderItems) {
            skuQuantities.merge(it.getSkuId(), it.getQuantity(), Integer::sum);
            BigDecimal lineTotal = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            skuRevenues.merge(it.getSkuId(), lineTotal, BigDecimal::add);
        }

        List<UUID> skuIds = new ArrayList<>(skuQuantities.keySet());
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        List<UUID> prodIds = skus.values().stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findAllById(prodIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        return skuQuantities.entrySet().stream()
                .map(e -> {
                    UUID skuId = e.getKey();
                    Sku sku = skus.get(skuId);
                    String code = sku != null ? sku.getCode() : "N/A";
                    String name = sku != null ? prodNames.getOrDefault(sku.getProductId(), code) : "N/A";
                    return new StockReportViews.TopSellingItem(
                            skuId, code, name, e.getValue(), skuRevenues.getOrDefault(skuId, BigDecimal.ZERO)
                    );
                })
                .sorted(Comparator.comparing(StockReportViews.TopSellingItem::quantitySold).reversed())
                .limit(limit > 0 ? limit : 10)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StockReportViews.SlowMovingItem> getSlowMoving(UUID tenantId, int days) {
        Instant since = Instant.now().minus(Math.max(days, 1), ChronoUnit.DAYS);

        // Tim cac SKU co don hang trong N ngay qua
        List<SalesOrder> recentOrders = salesOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("status").in(OrderStatus.CONFIRMED, OrderStatus.COMPLETED),
                cb.greaterThanOrEqualTo(root.get("createdAt"), since)
        ));
        Set<UUID> soldSkuIds = new HashSet<>();
        if (!recentOrders.isEmpty()) {
            List<UUID> orderIds = recentOrders.stream().map(SalesOrder::getId).toList();
            List<OrderItem> items = orderItemRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
            soldSkuIds = items.stream().map(OrderItem::getSkuId).collect(Collectors.toSet());
        }

        // Tim cac dong ton kho co on_hand > 0 ma khong nam trong soldSkuIds
        final Set<UUID> activeSoldSkuIds = soldSkuIds;
        List<StockBalance> slowBalances = stockBalanceRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.greaterThan(root.get("onHand"), 0)
        )).stream().filter(b -> !activeSoldSkuIds.contains(b.getSkuId())).toList();

        if (slowBalances.isEmpty()) {
            return List.of();
        }

        List<UUID> branchIds = slowBalances.stream().map(StockBalance::getBranchId).distinct().toList();
        Map<UUID, String> branchNames = branchRepository.findAllById(branchIds).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        List<UUID> skuIds = slowBalances.stream().map(StockBalance::getSkuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        List<UUID> prodIds = skus.values().stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findAllById(prodIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        return slowBalances.stream().map(b -> {
            Sku sku = skus.get(b.getSkuId());
            String code = sku != null ? sku.getCode() : "N/A";
            String name = sku != null ? prodNames.getOrDefault(sku.getProductId(), code) : "N/A";
            BigDecimal avgCost = (sku != null && sku.getAvgCost() != null) ? sku.getAvgCost() : BigDecimal.ZERO;
            BigDecimal totalVal = avgCost.multiply(BigDecimal.valueOf(b.getOnHand()));
            return new StockReportViews.SlowMovingItem(
                    b.getBranchId(),
                    branchNames.getOrDefault(b.getBranchId(), "N/A"),
                    b.getSkuId(),
                    code,
                    name,
                    b.getOnHand(),
                    avgCost,
                    totalVal,
                    days
            );
        }).sorted(Comparator.comparing(StockReportViews.SlowMovingItem::totalValue).reversed()).toList();
    }
}
