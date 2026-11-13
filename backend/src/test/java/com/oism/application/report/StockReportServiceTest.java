package com.oism.application.report;

import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesAnalyticsRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockReportServiceTest {

    @Mock StockBalanceRepository stockBalanceRepository;
    @Mock SkuRepository skuRepository;
    @Mock ProductRepository productRepository;
    @Mock BranchRepository branchRepository;
    @Mock SalesAnalyticsRepository salesAnalytics;

    StockReportService service;

    UUID tenantId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();
    UUID prodId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new StockReportService(
                stockBalanceRepository, skuRepository, productRepository,
                branchRepository, salesAnalytics);

        Branch branch = new Branch(tenantId, "Kho trung tâm", BranchType.WAREHOUSE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByTenantIdOrderByNameAsc(tenantId)).thenReturn(List.of(branch));

        Product product = new Product(tenantId, null, null, "Váy nữ mùa hè", null, false);
        ReflectionTestUtils.setField(product, "id", prodId);
        when(productRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(product));

        Sku sku = new Sku(tenantId, prodId, "DRESS-RED", Map.of(), null,
                BigDecimal.valueOf(500_000), BigDecimal.valueOf(450_000), 5);
        ReflectionTestUtils.setField(sku, "id", skuId);
        sku.setAvgCost(BigDecimal.valueOf(300_000));
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));
    }

    @Test
    @DisplayName("Báo cáo định giá tồn kho: on_hand * avg_cost")
    void testGetValuation() {
        StockBalance balance = new StockBalance(tenantId, branchId, skuId, 10, 2);

        when(stockBalanceRepository.findAll(any(Specification.class))).thenReturn(List.of(balance));

        StockReportViews.ValuationSummary valuation = service.getValuation(tenantId, null);

        assertNotNull(valuation);
        assertEquals(10, valuation.totalOnHand());
        assertEquals(BigDecimal.valueOf(3_000_000), valuation.totalInventoryValue());
        assertEquals(1, valuation.items().size());
        assertEquals(BigDecimal.valueOf(3_000_000), valuation.items().get(0).totalValue());
    }

    @Test
    @DisplayName("Top bán chạy tính từ đơn CONFIRMED/COMPLETED")
    void testGetTopSelling() {
        when(salesAnalytics.topSkusByQuantity(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(List.of(
                new SalesAnalyticsRepository.SkuSales(skuId, 5, BigDecimal.valueOf(2_500_000), BigDecimal.ZERO)));

        List<StockReportViews.TopSellingItem> top = service.getTopSelling(tenantId, 30, 10);

        assertFalse(top.isEmpty());
        assertEquals(5, top.get(0).quantitySold());
        assertEquals(0, BigDecimal.valueOf(2_500_000).compareTo(top.get(0).totalRevenue()));
        assertEquals("Váy nữ mùa hè", top.get(0).productName());
    }

    @Test
    @DisplayName("Hàng tồn chậm: còn tồn nhưng không có trong danh sách SKU đã bán")
    void testSlowMovingExcludesSoldSkus() {
        UUID soldSku = UUID.randomUUID();
        when(salesAnalytics.soldSkuIds(any(), any())).thenReturn(List.of(soldSku));
        when(stockBalanceRepository.findAll(any(Specification.class))).thenReturn(List.of(
                new StockBalance(tenantId, branchId, skuId, 7, 0),
                new StockBalance(tenantId, branchId, soldSku, 3, 0)));

        List<StockReportViews.SlowMovingItem> slow = service.getSlowMoving(tenantId, 30);

        assertEquals(1, slow.size());
        assertEquals(skuId, slow.get(0).skuId());
        assertEquals(0, BigDecimal.valueOf(2_100_000).compareTo(slow.get(0).totalValue()));
    }
}
