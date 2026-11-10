package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * FR-REP-01/02, FR-SIM-03, NFR-PERF-01: bao cao tong hop trong CSDL - dung so lieu tinh tay, dung bo loc,
 * co lap tenant, va chay duoc khi so don vuot 65.535 (truoc day loi 500 vi IN (...) qua nhieu tham so).
 */
class ProfitReportIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void profitReportMatchesHandCalculationAndFilters() {
        Session owner = newTenant();
        String skuA = stockedSku(owner, "PRA", 20, "60000");   // ban 100.000, von 60.000
        String skuB = stockedSku(owner, "PRB", 20, "30000");
        pos(owner, skuA, 2);   // DT 200.000, GV 120.000
        pos(owner, skuB, 1);   // DT 100.000, GV 30.000
        // don Draft khong tinh
        POST(owner, "/api/v1/orders", Map.of("branchId", owner.branchId(), "items", List.of(Map.of("skuId", skuA, "quantity", 5))), 201);

        JsonNode report = GET(owner, "/api/v1/reports/profit", 200);
        JsonNode summary = report.get("summary");
        assertMoney("300000", summary.get("totalRevenue"));
        assertMoney("150000", summary.get("totalCogs"));
        assertMoney("150000", summary.get("grossProfit"));
        assertEquals(0, new BigDecimal("0.5000").compareTo(summary.get("grossMargin").decimalValue()));
        assertEquals(2, summary.get("totalOrders").asInt());
        assertEquals(3, summary.get("totalQuantity").asInt());
        assertEquals(2, report.get("bySku").size());
        assertEquals("Ao PRA", report.get("bySku").get(0).get("productName").asText(), "Xep theo doanh thu giam dan");

        assertMoney("200000", GET(owner, "/api/v1/reports/profit?skuId=" + skuA, 200).get("summary").get("totalRevenue"));
        assertEquals(0, GET(owner, "/api/v1/reports/profit?channel=SHOPEE", 200).get("summary").get("totalOrders").asInt());
        assertEquals(2, GET(owner, "/api/v1/reports/profit?branchId=" + owner.branchId(), 200).get("summary").get("totalOrders").asInt());
        assertEquals(0, GET(owner, "/api/v1/reports/profit?to=2000-01-01T00:00:00Z", 200).get("summary").get("totalOrders").asInt());
        assertEquals("RP40001", GET(owner, "/api/v1/reports/profit?from=2026-12-31T00:00:00Z&to=2026-01-01T00:00:00Z", 400)
                .get("errorCode").asText());
        assertEquals(0, GET(newTenant(), "/api/v1/reports/profit", 200).get("summary").get("totalOrders").asInt());

        String csv = callRaw(owner, "/api/v1/reports/profit/export");
        assertTrue(csv.contains(",2,100000.00,60000.00,200000.00,120000.00,80000.00,40.00"), csv);
        assertTrue(csv.contains("\"TỔNG CỘNG\",,,,,3,,,300000.00,150000.00,150000.00,50.00"), csv);
    }

    @Test
    void reportsWorkWithMoreThan65kOrders() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "BIG", 5, "60000");
        int n = 70_000;
        jdbc.update("""
                INSERT INTO orders (id, tenant_id, branch_id, code, channel, status, total_amount, created_at, updated_at)
                SELECT gen_random_uuid(), ?::uuid, ?::uuid, 'BIG-' || g, 'POS', 'COMPLETED', 100000, now(), now()
                FROM generate_series(1, ?) g""", owner.tenantId(), owner.branchId(), n);
        jdbc.update("""
                INSERT INTO order_item (id, tenant_id, order_id, sku_id, quantity, unit_price, cost_price, created_at, updated_at)
                SELECT gen_random_uuid(), o.tenant_id, o.id, ?::uuid, 1, 100000, 60000, now(), now()
                FROM orders o WHERE o.tenant_id = ?::uuid AND o.code LIKE 'BIG-%'""", skuId, owner.tenantId());

        JsonNode summary = GET(owner, "/api/v1/reports/profit", 200).get("summary");
        assertEquals(n, summary.get("totalOrders").asInt());
        assertMoney(String.valueOf(n * 100_000L), summary.get("totalRevenue"));

        assertEquals(n, GET(owner, "/api/v1/reports/top-selling?days=1&limit=10", 200).get(0).get("quantitySold").asInt());
        GET(owner, "/api/v1/reports/slow-moving?days=1", 200);
        JsonNode daily = POST(owner, "/api/v1/reports/daily/run?date=" + LocalDate.now(ZoneOffset.UTC), null, 200);
        assertEquals(n, daily.get(0).get("orderCount").asInt());
    }

    private String stockedSku(Session owner, String code, int qty, String cost) {
        String skuId = newProduct(owner, "Ao " + code, code + "-" + UUID.randomUUID().toString().substring(0, 6),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, qty, cost);
        return skuId;
    }

    private void pos(Session owner, String skuId, int qty) {
        POST(owner, "/api/v1/pos/checkout", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", qty)), "paymentMethod", "CASH",
                "cashReceived", 10_000_000), 200);
    }

    private static void assertMoney(String expected, JsonNode actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual.decimalValue()), "mong doi " + expected + " nhung la " + actual);
    }

    private String callRaw(Session s, String url) {
        try {
            return mvc.perform(get(url).header("Authorization", "Bearer " + s.token())).andReturn()
                    .getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
