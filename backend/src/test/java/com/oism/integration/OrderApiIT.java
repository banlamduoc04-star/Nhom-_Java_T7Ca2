package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderApiIT extends AbstractIntegrationTest {

    @Test
    void createsDraftOrderWithTotalAndHistory() {
        Session s = newTenant();
        String sku1 = newProduct(s, "Ao Don Mot", "ORD-1", "100000").get("id").asText();
        String sku2 = newProduct(s, "Ao Don Hai", "ORD-2", "250000").get("id").asText();

        JsonNode order = POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(), "customerName", "Nguyen Van A",
                "customerPhone", "0901234567", "shippingAddress", "1 Le Loi",
                "items", List.of(Map.of("skuId", sku1, "quantity", 2), Map.of("skuId", sku2, "quantity", 1))), 201);

        assertEquals("DRAFT", order.get("status").asText());
        assertEquals("MANUAL", order.get("channel").asText());
        assertEquals(450000, order.get("totalAmount").asInt());
        assertEquals(true, order.get("reservedUntil").isNull());
        assertEquals(2, order.get("items").size());
        assertEquals(1, order.get("history").size());
        assertEquals("DRAFT", order.get("history").get(0).get("toStatus").asText());
        assertEquals(true, order.get("history").get(0).get("fromStatus").isNull());

        JsonNode detail = GET(s, "/api/v1/orders/" + order.get("id").asText(), 200);
        assertEquals(order.get("code").asText(), detail.get("code").asText());
    }

    @Test
    void validatesOrderInput() {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Kiem Don", "ORDV-1", "100000").get("id").asText();

        assertEquals("OD40001", POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(), "items", List.of()), 400)
                .get("errorCode").asText());
        assertEquals("OD40001", POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 0))), 400).get("errorCode").asText());
        assertEquals("OD40001", POST(s, "/api/v1/orders", Map.of("branchId", UUID.randomUUID(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 400).get("errorCode").asText());
        assertEquals("PR40401", POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", UUID.randomUUID(), "quantity", 1))), 404).get("errorCode").asText());
        assertEquals("OD40401", GET(s, "/api/v1/orders/" + UUID.randomUUID(), 404).get("errorCode").asText());

        PUT(s, "/api/v1/skus/" + skuId, Map.of("status", "INACTIVE"), 200);
        assertEquals("OD40001", POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 400).get("errorCode").asText());
    }

    @Test
    void listsOrdersWithFiltersAndPaging() {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Loc Don", "ORDL-1", "100000").get("id").asText();
        for (int i = 0; i < 3; i++) {
            POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(),
                    "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 201);
        }

        assertEquals(3, GET(s, "/api/v1/orders", 200).get("totalElements").asInt());
        assertEquals(2, GET(s, "/api/v1/orders?size=2", 200).get("content").size());
        assertEquals(1, GET(s, "/api/v1/orders?size=2&page=1", 200).get("content").size());
        assertEquals(3, GET(s, "/api/v1/orders?status=DRAFT&channel=MANUAL&branchId=" + s.branchId(), 200)
                .get("totalElements").asInt());
        assertEquals(0, GET(s, "/api/v1/orders?status=RESERVED", 200).get("totalElements").asInt());
        assertEquals(0, GET(s, "/api/v1/orders?channel=SHOPEE", 200).get("totalElements").asInt());
        // Ke hoach chi co POS/MANUAL/SHOPEE/TIKTOK: kenh khac la tham so khong hop le
        assertEquals("OD40001", GET(s, "/api/v1/orders?channel=LAZADA", 400).get("errorCode").asText());
        assertEquals(0, GET(s, "/api/v1/orders?channel=TIKTOK", 200).get("totalElements").asInt());
        assertEquals(0, GET(s, "/api/v1/orders?to=2000-01-01T00:00:00Z", 200).get("totalElements").asInt());
        assertEquals(3, GET(s, "/api/v1/orders?from=2000-01-01T00:00:00Z", 200).get("totalElements").asInt());
    }
}
