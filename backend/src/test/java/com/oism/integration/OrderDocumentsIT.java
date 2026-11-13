package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Danh sach API cua ke hoach: GET /orders/{id}/shipping-slip (Owner, Staff - FR-ORD-03)
 * va GET /pos/orders/{id}/receipt (Cashier, Owner - FR-POS-03). Tenant khac -> 404.
 */
class OrderDocumentsIT extends AbstractIntegrationTest {

    @Test
    void shippingSlipForOwnerAndStaffOnly() {
        Session owner = newTenant();
        String staff = newEmployee(owner, "STAFF");
        String cashier = newEmployee(owner, "CASHIER");
        String skuId = stockedSku(owner, "Ao Phieu Giao", 10);

        JsonNode order = POST(owner, "/api/v1/orders", Map.of("branchId", owner.branchId(),
                "customerName", "Khach <b>Giao</b>", "shippingAddress", "12 Le Loi",
                "items", List.of(Map.of("skuId", skuId, "quantity", 2))), 201);
        String url = "/api/v1/orders/" + order.get("id").asText() + "/shipping-slip";

        JsonNode slip = GET(owner, url, 200);
        assertEquals(order.get("code").asText(), slip.get("orderCode").asText());
        assertEquals("Ao Phieu Giao", slip.get("lines").get(0).get("productName").asText());
        assertEquals(2, slip.get("lines").get(0).get("quantity").asInt());
        assertEquals("Khach <b>Giao</b>", slip.get("customerName").asText(), "API tra du lieu tho, frontend tu escape");

        GET(staff, url, 200);
        assertEquals("AU40301", GET(cashier, url, 403).get("errorCode").asText());
        assertEquals("OD40401", GET(newTenant(), url, 404).get("errorCode").asText());

        POST(owner, "/api/v1/orders/" + order.get("id").asText() + "/cancel", Map.of("reason", "Khach doi y"), 200);
        assertEquals("OD40901", GET(owner, url, 409).get("errorCode").asText());
    }

    @Test
    void posReceiptReprintRespectsRoleBranchAndTenant() {
        Session owner = newTenant();
        String cashier = newEmployee(owner, "CASHIER");
        String staff = newEmployee(owner, "STAFF");
        String skuId = stockedSku(owner, "Ao Hoa Don", 10);

        JsonNode sold = POST(cashier, "/api/v1/pos/checkout", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 3)), "paymentMethod", "CASH",
                "cashReceived", 500000), 200);
        String url = "/api/v1/pos/orders/" + sold.get("orderId").asText() + "/receipt";

        JsonNode receipt = GET(cashier, url, 200);
        assertEquals(sold.get("orderCode").asText(), receipt.get("orderCode").asText());
        assertEquals(0, sold.get("totalAmount").decimalValue().compareTo(receipt.get("totalAmount").decimalValue()));
        assertEquals(0, sold.get("changeAmount").decimalValue().compareTo(receipt.get("changeAmount").decimalValue()));
        assertEquals("Ao Hoa Don", receipt.get("items").get(0).get("productName").asText());
        assertEquals(sold.get("cashierName").asText(), receipt.get("cashierName").asText());

        GET(owner, url, 200);
        assertEquals("AU40301", GET(staff, url, 403).get("errorCode").asText());
        assertEquals("OD40401", GET(newTenant(), url, 404).get("errorCode").asText());

        // Cashier cua chi nhanh khac khong xem duoc hoa don chi nhanh nay
        String otherBranch = POST(owner, "/api/v1/branches",
                Map.of("name", "CN Phu " + UUID.randomUUID().toString().substring(0, 4), "type", "STORE"), 201)
                .get("id").asText();
        String email = "cashier2-" + UUID.randomUUID() + "@test.vn";
        POST(owner, "/api/v1/users", Map.of("email", email, "password", PASSWORD, "fullName", "Thu ngan 2",
                "role", "CASHIER", "branchId", otherBranch), 201);
        assertEquals("AU40301", GET(login(email, PASSWORD), url, 403).get("errorCode").asText());

        // Don khong phai POS -> 404
        JsonNode manual = POST(owner, "/api/v1/orders", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 201);
        assertEquals("OD40401", GET(owner, "/api/v1/pos/orders/" + manual.get("id").asText() + "/receipt", 404)
                .get("errorCode").asText());
    }

    private String stockedSku(Session owner, String productName, int qty) {
        String skuId = newProduct(owner, productName, "DOC-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, qty, "60000");
        return skuId;
    }
}
