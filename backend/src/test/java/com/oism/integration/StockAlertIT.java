package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FR-REP-03 (UAT Sprint 4, kich ban 2): available <= reorder_point thi mo canh bao OPEN,
 * nhap them vuot nguong thi tu dong CLOSED; giu/nha hang don san cung mo/dong canh bao.
 * Moi (chi nhanh, SKU) toi da 1 canh bao OPEN.
 */
class StockAlertIT extends AbstractIntegrationTest {

    @Test
    void posSaleOpensAlertAndPurchaseReceiptClosesIt() {
        Session owner = newTenant();
        String skuId = skuWithReorderPoint(owner, 10);
        confirmedReceipt(owner, skuId, 11, "100000");
        assertEquals(0, alerts(owner, skuId, "OPEN").size(), "11 > 10: chua canh bao");

        POST(owner, "/api/v1/pos/checkout", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 2)), "paymentMethod", "CASH",
                "cashReceived", 1_000_000), 200);

        JsonNode open = alerts(owner, skuId, "OPEN");
        assertEquals(1, open.size(), "available 9 <= 10: mo dung 1 canh bao");
        assertEquals(9, open.get(0).get("available").asInt());
        assertEquals(10, open.get(0).get("threshold").asInt());

        // ban them van duoi nguong: khong mo trung
        POST(owner, "/api/v1/pos/checkout", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1)), "paymentMethod", "CASH",
                "cashReceived", 1_000_000), 200);
        assertEquals(1, alerts(owner, skuId, "OPEN").size());

        confirmedReceipt(owner, skuId, 50, "100000");

        assertEquals(0, alerts(owner, skuId, "OPEN").size(), "nhap 50 -> 58 > 10: tu dong dong");
        JsonNode closed = alerts(owner, skuId, "CLOSED");
        assertEquals(1, closed.size());
        assertTrue(closed.get(0).hasNonNull("closedAt"));
    }

    @Test
    void marketplaceReservationOpensAlertAndCancelClosesIt() {
        Session owner = newTenant();
        String skuId = skuWithReorderPoint(owner, 3);
        confirmedReceipt(owner, skuId, 5, "100000");
        String key = POST(owner, "/api/v1/settings/channel-keys/SHOPEE", null, 201).get("apiKey").asText();

        JsonNode order = body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/webhooks/shopee").header("X-Api-Key", key), null,
                Map.of("branchId", owner.branchId(), "externalOrderId", "SP-ALERT-1",
                        "items", List.of(Map.of("skuId", skuId, "quantity", 3))), 201));
        assertEquals(1, alerts(owner, skuId, "OPEN").size(), "giu 3 -> available 2 <= 3");

        POST(owner, "/api/v1/orders/" + order.get("id").asText() + "/cancel", Map.of("reason", "Khach huy"), 200);
        assertEquals(0, alerts(owner, skuId, "OPEN").size(), "nha hang -> available 5 > 3");
    }

    @Test
    void alertsAreTenantScoped() {
        Session a = newTenant();
        String skuId = skuWithReorderPoint(a, 100);
        confirmedReceipt(a, skuId, 5, "100000");

        assertEquals(1, alerts(a, skuId, "OPEN").size());
        String alertId = alerts(a, skuId, "OPEN").get(0).get("id").asText();

        Session b = newTenant();
        assertEquals(0, GET(b, "/api/v1/alerts/stock", 200).size());
        assertEquals("IV40401", POST(b, "/api/v1/alerts/stock/" + alertId + "/resolve", null, 404)
                .get("errorCode").asText());
    }

    private String skuWithReorderPoint(Session owner, int reorderPoint) {
        Map<String, Object> sku = new LinkedHashMap<>();
        sku.put("code", "ALERT-" + UUID.randomUUID().toString().substring(0, 8));
        sku.put("retailPrice", "150000");
        sku.put("reorderPoint", reorderPoint);
        JsonNode product = POST(owner, "/api/v1/products",
                Map.of("name", "Ao Canh Bao", "hasVariants", false, "skus", List.of(sku)), 201);
        return product.get("skus").get(0).get("id").asText();
    }

    private JsonNode alerts(Session owner, String skuId, String status) {
        JsonNode all = GET(owner, "/api/v1/alerts/stock?status=" + status, 200);
        List<JsonNode> matching = new java.util.ArrayList<>();
        all.forEach(n -> {
            if (skuId.equals(n.get("skuId").asText())) {
                matching.add(n);
            }
        });
        return json.valueToTree(matching);
    }
}
