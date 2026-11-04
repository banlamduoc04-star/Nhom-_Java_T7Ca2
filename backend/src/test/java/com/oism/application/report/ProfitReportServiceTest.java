package com.oism.application.report;

import com.oism.application.report.ProfitReportService.ProfitSummary;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProfitReportServiceTest {

    @Mock SalesOrderRepository orderRepository;
    @Mock OrderItemRepository orderItemRepository;
    @Mock SkuRepository skuRepository;
    @Mock BranchRepository branchRepository;

    ProfitReportService service;

    UUID tenantId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new ProfitReportService(orderRepository, orderItemRepository, skuRepository, branchRepository);

        SalesOrder order = new SalesOrder(tenantId, branchId, "DH-001", OrderChannel.POS, null,
                OrderStatus.CONFIRMED, "Khách hàng", "0900000000", null, BigDecimal.valueOf(200), null);
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "createdAt", Instant.now());
        when(orderRepository.findAll(any(Specification.class))).thenReturn(List.of(order));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 2, BigDecimal.valueOf(100.00));
        item.setCostPrice(BigDecimal.valueOf(60.00)); // Cost = 120, Revenue = 200, Profit = 80, Margin = 40%
        when(orderItemRepository.findByTenantIdAndOrderIdIn(any(), any())).thenReturn(List.of(item));

        Sku sku = new Sku(tenantId, UUID.randomUUID(), "POLO-L", Map.of(), null, BigDecimal.valueOf(100), null, 0);
        ReflectionTestUtils.setField(sku, "id", skuId);
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));

        Branch branch = new Branch(tenantId, "Chi nhánh 1", BranchType.STORE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByTenantId(tenantId)).thenReturn(List.of(branch));
    }

    @Test
    void getProfitReport_calculatesAccurately() {
        ProfitSummary summary = service.getProfitReport(tenantId, null, null, null, null, null);

        assertEquals(BigDecimal.valueOf(200.00).setScale(2), summary.totalRevenue().setScale(2));
        assertEquals(BigDecimal.valueOf(120.00).setScale(2), summary.totalCost().setScale(2));
        assertEquals(BigDecimal.valueOf(80.00).setScale(2), summary.totalGrossProfit().setScale(2));
        assertEquals(BigDecimal.valueOf(40.00).setScale(2), summary.grossMarginPercent().setScale(2));
        assertEquals(1, summary.totalOrders());
        assertEquals(2, summary.totalQuantity());
        assertEquals(1, summary.rows().size());
    }

    @Test
    void exportCsv_generatesProperCsvHeaderAndValues() {
        byte[] csvBytes = service.exportCsv(tenantId, null, null, null, null, null);
        String csv = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csv.contains("Mã đơn,Chi nhánh,Kênh,Mã SKU"));
        assertTrue(csv.contains("DH-001"));
        assertTrue(csv.contains("POLO-L"));
        assertTrue(csv.contains("TỔNG CỘNG"));
    }
}
