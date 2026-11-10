package com.oism.application.report;

import com.oism.domain.model.Branch;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesAnalyticsRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final SalesAnalyticsRepository salesAnalytics;

    public StockReportService(StockBalanceRepository stockBalanceRepository,
                              SkuRepository skuRepository,
                              ProductRepository productRepository,
                              BranchRepository branchRepository,
                              SalesAnalyticsRepository salesAnalytics) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
        this.salesAnalytics = salesAnalytics;
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

        Map<UUID, String> branchNames = branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));

        List<UUID> skuIds = balances.stream().map(StockBalance::getSkuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        Map<UUID, String> prodNames = productNames(tenantId, skus.values());

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

    /** Top ban chay theo so luong (kem doanh thu), tong hop trong CSDL (NFR-PERF-01). */
    @Transactional(readOnly = true)
    public List<StockReportViews.TopSellingItem> getTopSelling(UUID tenantId, int days, int limit) {
        Instant since = Instant.now().minus(Math.max(days, 1), ChronoUnit.DAYS);
        List<SalesAnalyticsRepository.SkuSales> top = salesAnalytics.topSkusByQuantity(
                tenantId, SalesAnalyticsRepository.SalesFilter.since(since), limit > 0 ? Math.min(limit, 100) : 10);
        if (top.isEmpty()) {
            return List.of();
        }
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId,
                top.stream().map(SalesAnalyticsRepository.SkuSales::skuId).toList()).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        Map<UUID, String> prodNames = productNames(tenantId, skus.values());
        return top.stream().map(t -> {
            Sku sku = skus.get(t.skuId());
            String code = sku != null ? sku.getCode() : "N/A";
            String name = sku != null ? prodNames.getOrDefault(sku.getProductId(), code) : "N/A";
            return new StockReportViews.TopSellingItem(t.skuId(), code, name, (int) t.quantity(),
                    t.revenue().setScale(2, RoundingMode.HALF_UP));
        }).toList();
    }

    /** Hang ton cham: con ton (on_hand > 0) nhung khong ban trong N ngay. */
    @Transactional(readOnly = true)
    public List<StockReportViews.SlowMovingItem> getSlowMoving(UUID tenantId, int days) {
        Instant since = Instant.now().minus(Math.max(days, 1), ChronoUnit.DAYS);
        Set<UUID> soldSkuIds = new HashSet<>(salesAnalytics.soldSkuIds(tenantId,
                SalesAnalyticsRepository.SalesFilter.since(since)));

        List<StockBalance> slowBalances = stockBalanceRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.greaterThan(root.get("onHand"), 0)
        )).stream().filter(b -> !soldSkuIds.contains(b.getSkuId())).toList();
        if (slowBalances.isEmpty()) {
            return List.of();
        }

        Map<UUID, String> branchNames = branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId,
                slowBalances.stream().map(StockBalance::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        Map<UUID, String> prodNames = productNames(tenantId, skus.values());

        return slowBalances.stream().map(b -> {
            Sku sku = skus.get(b.getSkuId());
            String code = sku != null ? sku.getCode() : "N/A";
            String name = sku != null ? prodNames.getOrDefault(sku.getProductId(), code) : "N/A";
            BigDecimal avgCost = (sku != null && sku.getAvgCost() != null) ? sku.getAvgCost() : BigDecimal.ZERO;
            return new StockReportViews.SlowMovingItem(b.getBranchId(), branchNames.getOrDefault(b.getBranchId(), "N/A"),
                    b.getSkuId(), code, name, b.getOnHand(), avgCost,
                    avgCost.multiply(BigDecimal.valueOf(b.getOnHand())), days);
        }).sorted(Comparator.comparing(StockReportViews.SlowMovingItem::totalValue).reversed()).toList();
    }

    private Map<UUID, String> productNames(UUID tenantId, java.util.Collection<Sku> skus) {
        return productRepository.findByTenantIdAndIdIn(tenantId,
                        skus.stream().map(Sku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));
    }
}
