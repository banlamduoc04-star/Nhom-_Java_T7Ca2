package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** FR-AUTH-03..05: phan quyen theo vai tro, quan ly chi nhanh va nhan vien. */
class RbacIT extends AbstractIntegrationTest {

    @Test
    void anonymousGets401AndWrongRoleGets403() {
        Session owner = newTenant();
        String staff = newEmployee(owner, "STAFF");
        String cashier = newEmployee(owner, "CASHIER");

        // Khong token -> 401 AU40101
        JsonNode unauthorized = body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/branches"), null, null, 401));
        assertEquals("AU40101", unauthorized.get("errorCode").asText());

        // Cashier goi API Owner -> 403 AU40301
        assertEquals("AU40301", POST(cashier, "/api/v1/categories", Map.of("name", "X"), 403).get("errorCode").asText());
        assertEquals("AU40301", POST(cashier, "/api/v1/branches", Map.of("name", "X", "type", "STORE"), 403)
                .get("errorCode").asText());
        GET(cashier, "/api/v1/users", 403);
        GET(cashier, "/api/v1/purchase-receipts", 403);
        GET(cashier, "/api/v1/inventory-transactions", 403);

        // Staff: duoc kho/don, khong duoc danh muc/nhan vien
        GET(staff, "/api/v1/purchase-receipts", 200);
        GET(staff, "/api/v1/inventory-transactions", 200);
        POST(staff, "/api/v1/categories", Map.of("name", "X"), 403);
        GET(staff, "/api/v1/users", 403);

        // Moi vai tro xem duoc danh muc, thuong hieu, chi nhanh, san pham, ton kho
        for (String token : List.of(staff, cashier, owner.token())) {
            GET(token, "/api/v1/categories", 200);
            GET(token, "/api/v1/brands", 200);
            GET(token, "/api/v1/branches", 200);
            GET(token, "/api/v1/products", 200);
            GET(token, "/api/v1/stock", 200);
            GET(token, "/api/v1/me", 200);
        }
    }

    @Test
    void ownerManagesBranchesAndEmployees() {
        Session owner = newTenant();

        JsonNode branch = POST(owner, "/api/v1/branches", Map.of("name", "Kho Tong", "type", "WAREHOUSE"), 201);
        assertEquals("AU40901", POST(owner, "/api/v1/branches", Map.of("name", "kho tong", "type", "STORE"), 409)
                .get("errorCode").asText());
        assertEquals("Kho Moi", PUT(owner, "/api/v1/branches/" + branch.get("id").asText(),
                Map.of("name", "Kho Moi", "type", "WAREHOUSE", "address", "Q7"), 200).get("name").asText());
        assertEquals(2, GET(owner, "/api/v1/branches", 200).size());

        // Cashier bat buoc co chi nhanh; vai tro Owner khong tao qua API
        assertEquals("AU40001", POST(owner, "/api/v1/users", Map.of("email", "c1@test.vn", "password", PASSWORD,
                "fullName", "Thu ngan", "role", "CASHIER"), 400).get("errorCode").asText());
        assertEquals("AU40001", POST(owner, "/api/v1/users", Map.of("email", "o2@test.vn", "password", PASSWORD,
                "fullName", "Chu 2", "role", "OWNER"), 400).get("errorCode").asText());

        JsonNode user = POST(owner, "/api/v1/users", Map.of("email", "nv-" + System.nanoTime() + "@test.vn",
                "password", PASSWORD, "fullName", "Thu ngan 1", "role", "CASHIER", "branchId", branch.get("id").asText()), 201);
        String userId = user.get("id").asText();

        // Khoa tai khoan -> dang nhap bi tu choi AU40302; mo khoa -> dang nhap lai duoc
        String email = user.get("email").asText();
        PATCH(owner, "/api/v1/users/" + userId + "/status", Map.of("active", false), 200);
        assertEquals("AU40302", body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/login"), null, Map.of("identifier", email, "password", PASSWORD), 403))
                .get("errorCode").asText());
        PATCH(owner, "/api/v1/users/" + userId + "/status", Map.of("active", true), 200);

        // Dat lai mat khau
        call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/users/" + userId + "/password"),
                owner.token(), Map.of("newPassword", "NewPass@123"), 204);
        login(email, "NewPass@123");

        // Owner khong tu khoa minh duoc
        String ownerId = GET(owner, "/api/v1/me", 200).get("userId").asText();
        PATCH(owner, "/api/v1/users/" + ownerId + "/status", Map.of("active", false), 403);
    }

    @Test
    void cannotHideBranchWithStockOrOpenOrders() {
        Session owner = newTenant();
        JsonNode sku = newProduct(owner, "Ao Kho", "KHO-01", "100000");
        String skuId = sku.get("id").asText();

        JsonNode second = POST(owner, "/api/v1/branches", Map.of("name", "Chi nhanh 2", "type", "STORE"), 201);
        String secondId = second.get("id").asText();

        // Chi nhanh trong, khong don -> an duoc
        PATCH(owner, "/api/v1/branches/" + secondId + "/status", Map.of("active", false), 200);
        PATCH(owner, "/api/v1/branches/" + secondId + "/status", Map.of("active", true), 200);

        // Co don Draft chua ket thuc -> khong an duoc
        POST(owner, "/api/v1/orders", Map.of("branchId", secondId,
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 201);
        assertEquals("AU40902", PATCH(owner, "/api/v1/branches/" + secondId + "/status", Map.of("active", false), 409)
                .get("errorCode").asText());

        // Co ton kho -> khong an duoc
        confirmedReceipt(owner, skuId, 3, "50000");
        assertEquals("AU40902", PATCH(owner, "/api/v1/branches/" + owner.branchId() + "/status",
                Map.of("active", false), 409).get("errorCode").asText());
    }
    @Test
    void cannotHideLastActiveBranch() {
        Session owner = newTenant();
        assertEquals("AU40903", PATCH(owner, "/api/v1/branches/" + owner.branchId() + "/status",
                Map.of("active", false), 409).get("errorCode").asText());
    }
}

