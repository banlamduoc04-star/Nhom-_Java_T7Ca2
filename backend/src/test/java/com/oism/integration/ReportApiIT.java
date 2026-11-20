package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * FR-REP-02 theo Danh sach API cua ke hoach (/reports/inventory-value, /top-selling, /slow-moving: Owner, Staff)
 * va FR-SIM-03 daily_report theo mo hinh du lieu (chi nhanh x ngay x kenh).
 */
class ReportApiIT extends AbstractIntegrationTest {

    @Test
    void stockReportsFollowPlanPathsAndRoles() {
        Session owner = newTenant();
        String staff = newEmployee(owner, "STAFF");
        String cashier = newEmployee(owner, "CASHIER");
        String skuId = newProduct(owner, "Ao Bao Cao", "RP-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, 10, "60000");

        JsonNode value = GET(staff, "/api/v1/reports/inventory-value", 200);
        assertEquals(0, value.get("totalInventoryValue").decimalValue().compareTo(new java.math.BigDecimal("600000")),
                "Gia tri ton = 10 x 60.000");
        GET(staff, "/api/v1/reports/top-selling?days=30&limit=10", 200);
        GET(staff, "/api/v1/reports/slow-moving?days=30", 200);
        GET(owner, "/api/v1/reports/inventory-value", 200);

        assertEquals("AU40301", GET(cashier, "/api/v1/reports/inventory-value", 403).get("errorCode").asText());
        // duong dan cu khong con
        body(call(get("/api/v1/reports/stock/valuation"), owner.token(), null, 404));
        // endpoint dashboard-summary (khong loc tenant, so lieu gia) da go
        body(call(get("/api/v1/reports/dashboard-summary"), owner.token(), null, 404));
    }

    @Test
    void dailyReportGroupsByBranchAndChannelAndRerunReplaces() {
        Session owner = newTenant();
        String skuId = newProduct(owner, "Ao Chot Ngay", "DR-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, 10, "60000");
        POST(owner, "/api/v1/pos/checkout", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 2)), "paymentMethod", "CASH",
                "cashReceived", 500000), 200);
        // don Draft khong tinh doanh thu
        POST(owner, "/api/v1/orders", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 201);

        String today = LocalDate.now(ZoneOffset.UTC).toString();
        JsonNode rows = POST(owner, "/api/v1/reports/daily/run?date=" + today, null, 200);
        assertEquals(1, rows.size(), "Chi 1 nhom: chi nhanh mac dinh x POS");
        JsonNode row = rows.get(0);
        assertEquals("POS", row.get("channel").asText());
        assertEquals(owner.branchId(), row.get("branchId").asText());
        assertEquals(1, row.get("orderCount").asInt());
        assertEquals(0, row.get("revenue").decimalValue().compareTo(new java.math.BigDecimal("200000")));
        assertEquals(0, row.get("cogs").decimalValue().compareTo(new java.math.BigDecimal("120000")));
        assertEquals(0, row.get("grossProfit").decimalValue().compareTo(new java.math.BigDecimal("80000")));

        // chot lai cung ngay: ghi de, khong trung (unique tenant, branch, date, channel)
        POST(owner, "/api/v1/reports/daily/run?date=" + today, null, 200);
        assertEquals(1, GET(owner, "/api/v1/reports/daily", 200).size());

        assertEquals("AU40301", GET(newEmployee(owner, "STAFF"), "/api/v1/reports/daily", 403)
                .get("errorCode").asText());
        assertEquals(0, GET(newTenant(), "/api/v1/reports/daily", 200).size());
    }
}
