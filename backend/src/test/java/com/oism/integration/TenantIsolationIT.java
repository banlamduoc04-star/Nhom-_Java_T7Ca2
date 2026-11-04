package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NFR-TENANT-01: nguoi dung tenant B goi du lieu tenant A phai nhan 404, danh sach khong lo du lieu cheo. */
class TenantIsolationIT extends AbstractIntegrationTest {

    @Test
    void tenantBCannotReadOrChangeTenantAData() {
        Session a = newTenant();
        Session b = newTenant();

        String categoryId = POST(a, "/api/v1/categories", Map.of("name", "Danh muc A"), 201).get("id").asText();
        String brandId = POST(a, "/api/v1/brands", Map.of("name", "Brand A"), 201).get("id").asText();
        JsonNode sku = newProduct(a, "Ao Thun A", "A-001", "100000");
        String productId = sku.get("productId").asText();
        String skuId = sku.get("id").asText();
        String barcode = POST(a, "/api/v1/skus/" + skuId + "/barcode", null, 200).get("barcode").asText();
        String receiptId = POST(a, "/api/v1/purchase-receipts", Map.of("branchId", a.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 5, "unitPrice", "60000"))), 201).get("id").asText();
        String orderId = POST(a, "/api/v1/orders", Map.of("branchId", a.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 201).get("id").asText();

        // Doc cheo -> 404
        GET(b, "/api/v1/products/" + productId, 404);
        GET(b, "/api/v1/purchase-receipts/" + receiptId, 404);
        GET(b, "/api/v1/orders/" + orderId, 404);
        GET(b, "/api/v1/skus/lookup?barcode=" + barcode, 404);

        // Ghi cheo -> 404
        PUT(b, "/api/v1/categories/" + categoryId, Map.of("name", "Doi ten"), 404);
        PUT(b, "/api/v1/brands/" + brandId, Map.of("name", "Doi ten"), 404);
        PUT(b, "/api/v1/skus/" + skuId + "/prices", Map.of("retailPrice", "1"), 404);
        POST(b, "/api/v1/purchase-receipts/" + receiptId + "/confirm", null, 404);
        PATCH(b, "/api/v1/branches/" + a.branchId() + "/status", Map.of("active", false), 404);
        DELETE(b, "/api/v1/categories/" + categoryId, 404);

        // Dung id cua A khi tao du lieu cua B -> khong tim thay
        POST(b, "/api/v1/products", Map.of("name", "Ao Thun B", "categoryId", categoryId, "hasVariants", false,
                "skus", List.of(Map.of("code", "B-001", "retailPrice", "1000"))), 404);
        POST(b, "/api/v1/purchase-receipts", Map.of("branchId", a.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1, "unitPrice", "1"))), 400);
        POST(b, "/api/v1/orders", Map.of("branchId", b.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1))), 404);

        // Danh sach cua B khong chua du lieu cua A
        assertEquals(0, GET(b, "/api/v1/products", 200).get("totalElements").asInt());
        assertEquals(0, GET(b, "/api/v1/orders", 200).get("totalElements").asInt());
        assertEquals(0, GET(b, "/api/v1/purchase-receipts", 200).get("totalElements").asInt());
        assertEquals(0, GET(b, "/api/v1/stock", 200).get("totalElements").asInt());
        assertEquals(0, GET(b, "/api/v1/categories", 200).size());
        assertEquals(0, GET(b, "/api/v1/brands", 200).size());
        assertEquals(1, GET(b, "/api/v1/branches", 200).size());
        assertEquals(1, GET(b, "/api/v1/users", 200).size());

        // Du lieu cua A van nguyen ven
        assertEquals("Danh muc A", GET(a, "/api/v1/categories", 200).get(0).get("name").asText());
        assertTrue(GET(a, "/api/v1/branches", 200).get(0).get("active").asBoolean());
    }

    @Test
    void sameSkuCodeAndBarcodeMayExistInDifferentTenants() {
        Session a = newTenant();
        Session b = newTenant();

        newProduct(a, "Ao Chung", "CHUNG-01", "100000");
        JsonNode skuB = newProduct(b, "Ao Chung", "CHUNG-01", "200000");

        assertFalse(skuB.get("id").asText().isBlank());
        assertEquals(1, GET(a, "/api/v1/products?keyword=CHUNG", 200).get("totalElements").asInt());
        assertEquals(1, GET(b, "/api/v1/products?keyword=CHUNG", 200).get("totalElements").asInt());
    }
}
