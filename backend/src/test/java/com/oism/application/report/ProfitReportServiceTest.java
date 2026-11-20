package com.oism.application.report;

import com.oism.application.report.ProfitReportService.ProfitReportView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesAnalyticsRepository;
import com.oism.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/** FR-REP-01: so lieu tinh tay - 2 cai gia ban 100, gia von 60 -> doanh thu 200, gia von 120, LN 80, bien 40%. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfitReportServiceTest {

    @Mock SalesAnalyticsRepository salesAnalytics;
    @Mock SkuRepository skuRepository;
    @Mock ProductRepository productRepository;
    @Mock BranchRepository branchRepository;

    ProfitReportService service;

    UUID tenantId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();
    UUID productId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new ProfitReportService(salesAnalytics, skuRepository, productRepository, branchRepository);

        when(salesAnalytics.totals(eq(tenantId), any())).thenReturn(new SalesAnalyticsRepository.SalesTotals(
                1, 2, BigDecimal.valueOf(200), BigDecimal.valueOf(120)));
        when(salesAnalytics.bySku(eq(tenantId), any())).thenReturn(List.of(new SalesAnalyticsRepository.SkuSales(
                skuId, 2, BigDecimal.valueOf(200), BigDecimal.valueOf(120))));
        when(salesAnalytics.lines(eq(tenantId), any())).thenReturn(List.of(new SalesAnalyticsRepository.SalesLine(
                "DH-001", branchId, OrderChannel.POS, skuId, 2, BigDecimal.valueOf(100), BigDecimal.valueOf(60),
                Instant.parse("2026-11-10T03:00:00Z"))));

        Sku sku = new Sku(tenantId, productId, "POLO-L", Map.of(), null, BigDecimal.valueOf(100), null, 0);
        ReflectionTestUtils.setField(sku, "id", skuId);
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));
        Product product = new Product(tenantId, null, null, "Ao Polo", null, false);
        ReflectionTestUtils.setField(product, "id", productId);
        when(productRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(product));
        Branch branch = new Branch(tenantId, "Chi nhánh 1", BranchType.STORE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByTenantId(tenantId)).thenReturn(List.of(branch));
    }

    @Test
    void summaryAndBySkuMatchHandCalculation() {
        ProfitReportView report = service.getProfitReport(tenantId, null, null, null, null, null);

        assertEquals(new BigDecimal("200.00"), report.summary().totalRevenue());
        assertEquals(new BigDecimal("120.00"), report.summary().totalCogs());
        assertEquals(new BigDecimal("80.00"), report.summary().grossProfit());
        assertEquals(new BigDecimal("0.4000"), report.summary().grossMargin());
        assertEquals(1, report.summary().totalOrders());
        assertEquals(2, report.summary().totalQuantity());

        assertEquals(1, report.bySku().size());
        assertEquals("POLO-L", report.bySku().get(0).skuCode());
        assertEquals("Ao Polo", report.bySku().get(0).productName());
        assertEquals(new BigDecimal("80.00"), report.bySku().get(0).grossProfit());
    }

    @Test
    void zeroRevenueGivesZeroMarginNotDivisionError() {
        when(salesAnalytics.totals(eq(tenantId), any())).thenReturn(
                new SalesAnalyticsRepository.SalesTotals(0, 0, null, null));
        when(salesAnalytics.bySku(eq(tenantId), any())).thenReturn(List.of());

        ProfitReportView report = service.getProfitReport(tenantId, null, null, null, null, null);

        assertEquals(new BigDecimal("0.00"), report.summary().totalRevenue());
        assertEquals(0, report.summary().grossMargin().signum());
    }

    @Test
    void csvHasDotDecimalsAndTotals() {
        String csv = new String(service.exportCsv(tenantId, null, null, null, null, null), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("﻿Mã đơn,Ngày,Chi nhánh,Kênh,Mã SKU"));
        assertTrue(csv.contains("\"DH-001\",2026-11-10T03:00:00Z,\"Chi nhánh 1\",POS,\"POLO-L\",2,100.00,60.00,200.00,120.00,80.00,40.00"),
                csv);
        assertTrue(csv.contains("\"TỔNG CỘNG\",,,,,2,,,200.00,120.00,80.00,40.00"), csv);
    }

    @Test
    void invalidDateRangeIsRP40001() {
        BusinessException ex = assertThrows(BusinessException.class, () -> service.getProfitReport(tenantId,
                Instant.parse("2026-11-05T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"), null, null, null));
        assertEquals(ErrorCode.RP40001, ex.getErrorCode());
    }
}
