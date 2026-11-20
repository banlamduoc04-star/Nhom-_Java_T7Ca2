package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** FR-POS-04, FR-AUTH-05: POS cho Cashier (chi nhanh duoc gan) va Owner; Staff khong dung POS; Simulator chi Owner. */
class PosCheckoutIT extends AbstractIntegrationTest {

    @Test
    void cashierSellsOnlyAtAssignedBranchOwnerAnywhereStaffNever() {
        Session owner = newTenant();
        String cashier = newEmployee(owner, "CASHIER"); // gan chi nhanh mac dinh
        String staff = newEmployee(owner, "STAFF");
        String otherBranch = POST(owner, "/api/v1/branches",
                Map.of("name", "CN Hai " + UUID.randomUUID().toString().substring(0, 4), "type", "STORE"), 201)
                .get("id").asText();

        String skuId = newProduct(owner, "Ao Pos Test", "POS-" + UUID.randomUUID().toString().substring(0, 6),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, 10, "60000");

        JsonNode sold = POST(cashier, "/api/v1/pos/checkout", checkout(owner.branchId(), skuId), 200);
        assertEquals("COMPLETED", sold.get("status").asText());

        assertEquals("AU40301", POST(cashier, "/api/v1/pos/checkout", checkout(otherBranch, skuId), 403)
                .get("errorCode").asText());
        assertEquals("AU40301", POST(staff, "/api/v1/pos/checkout", checkout(owner.branchId(), skuId), 403)
                .get("errorCode").asText());
        POST(owner, "/api/v1/pos/checkout", checkout(owner.branchId(), skuId), 200);
    }

    @Test
    void simulatorIsOwnerOnly() {
        Session owner = newTenant();
        String staff = newEmployee(owner, "STAFF");
        Map<String, Object> req = Map.of("branchId", owner.branchId(), "channel", "SHOPEE",
                "skuId", UUID.randomUUID().toString(), "quantity", 1, "orderCount", 1);

        assertEquals("AU40301", POST(staff, "/api/v1/simulator/orders", req, 403).get("errorCode").asText());
    }

    private static Map<String, Object> checkout(String branchId, String skuId) {
        return Map.of("branchId", branchId, "items", List.of(Map.of("skuId", skuId, "quantity", 1)),
                "paymentMethod", "CASH", "cashReceived", 200000);
    }
}
