package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.oism.domain.service.Ean13;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FR-PROD-01..05: danh muc, thuong hieu, san pham/SKU, ma vach, gia, tim kiem. */
class ProductApiIT extends AbstractIntegrationTest {

    private static Map<String, Object> sku(String code, String retail) {
        return Map.of("code", code, "retailPrice", retail, "attributes", Map.of("size", code));
    }

    @Test
    void createsProductWithThreeVariantsInOneCall() {
        Session s = newTenant();
        String categoryId = POST(s, "/api/v1/categories", Map.of("name", "Ao"), 201).get("id").asText();
        String brandId = POST(s, "/api/v1/brands", Map.of("name", "Nike"), 201).get("id").asText();

        JsonNode product = POST(s, "/api/v1/products", Map.of("name", "Ao Polo Nam", "categoryId", categoryId,
                "brandId", brandId, "hasVariants", true,
                "skus", List.of(sku("POLO-S", "150000"), sku("POLO-M", "150000"), sku("POLO-L", "160000"))), 201);

        assertEquals(3, product.get("skus").size());
        assertEquals("ACTIVE", product.get("status").asText());
        JsonNode detail = GET(s, "/api/v1/products/" + product.get("id").asText(), 200);
        assertEquals(3, detail.get("skus").size());
        assertEquals("POLO-L", detail.get("skus").get(0).get("code").asText());   // sap theo ma SKU
        assertEquals("POLO-L", detail.get("skus").get(0).get("attributes").get("size").asText());
    }

    @Test
    void validatesNameSkuPricesAndSingleProductRule() {
        Session s = newTenant();

        // Ten sai quy tac -> 400 PR40001 kem details
        JsonNode badName = POST(s, "/api/v1/products", Map.of("name", "ao thun", "hasVariants", false,
                "skus", List.of(sku("X-1", "1000"))), 400);
        assertEquals("PR40001", badName.get("errorCode").asText());
        assertEquals("name", badName.get("details").get(0).get("field").asText());

        // Gia <= 0
        assertEquals("PR40001", POST(s, "/api/v1/products", Map.of("name", "Ao Hop Le", "hasVariants", false,
                "skus", List.of(sku("X-2", "0"))), 400).get("errorCode").asText());

        // San pham don co dung 1 SKU
        assertEquals("PR40001", POST(s, "/api/v1/products", Map.of("name", "Ao Don Le", "hasVariants", false,
                "skus", List.of(sku("X-3", "1000"), sku("X-4", "1000"))), 400).get("errorCode").asText());

        // Them SKU thu 2 vao san pham don bi chan
        String productId = newProduct(s, "Ao Mot Sku", "ONE-1", "1000").get("productId").asText();
        assertEquals("PR40001", POST(s, "/api/v1/products/" + productId + "/skus", sku("ONE-2", "1000"), 400)
                .get("errorCode").asText());
    }

    @Test
    void rejectsDuplicateSkuCodeAndBarcode() {
        Session s = newTenant();
        newProduct(s, "Ao Trung", "DUP-1", "1000");

        assertEquals("PR40901", POST(s, "/api/v1/products", Map.of("name", "Ao Trung Hai", "hasVariants", false,
                "skus", List.of(sku("DUP-1", "1000"))), 409).get("errorCode").asText());

        POST(s, "/api/v1/products", Map.of("name", "Ao Co Ma Vach", "hasVariants", false, "skus", List.of(
                Map.of("code", "BC-1", "retailPrice", "1000", "barcode", "CODE128-ABC"))), 201);
        assertEquals("PR40901", POST(s, "/api/v1/products", Map.of("name", "Ao Ma Vach Hai", "hasVariants", false,
                "skus", List.of(Map.of("code", "BC-2", "retailPrice", "1000", "barcode", "CODE128-ABC"))), 409)
                .get("errorCode").asText());
    }

    @Test
    void generatesValidEan13AndLooksUpByBarcode() {
        Session s = newTenant();
        JsonNode sku = newProduct(s, "Ao Quet Ma", "SCAN-1", "99000");
        String skuId = sku.get("id").asText();

        String barcode = POST(s, "/api/v1/skus/" + skuId + "/barcode", null, 200).get("barcode").asText();
        assertTrue(Ean13.isValid(barcode), "EAN-13 phai co check digit dung: " + barcode);
        // Da co ma vach thi khong sinh lai
        assertEquals("PR40901", POST(s, "/api/v1/skus/" + skuId + "/barcode", null, 409).get("errorCode").asText());

        confirmedReceipt(s, skuId, 7, "50000");
        JsonNode lookup = GET(s, "/api/v1/skus/lookup?barcode=" + barcode, 200);
        assertEquals("SCAN-1", lookup.get("sku").get("code").asText());
        assertEquals(7, lookup.get("available").asInt());
        assertEquals("PR40401", GET(s, "/api/v1/skus/lookup?barcode=0000000000000", 404).get("errorCode").asText());
    }

    @Test
    void searchesByNameSkuAndBarcodeWithPaging() {
        Session s = newTenant();
        for (int i = 1; i <= 25; i++) {
            newProduct(s, "Ao Mau " + i, "MAU-" + String.format("%02d", i), "1000");
        }
        newProduct(s, "Quan Jean", "JEAN-X", "2000");
        String skuId = GET(s, "/api/v1/products?keyword=JEAN-X", 200).get("content").get(0).get("id").asText();
        String jeanSku = GET(s, "/api/v1/products/" + skuId, 200).get("skus").get(0).get("id").asText();
        String barcode = POST(s, "/api/v1/skus/" + jeanSku + "/barcode", null, 200).get("barcode").asText();

        JsonNode page0 = GET(s, "/api/v1/products", 200);
        assertEquals(20, page0.get("size").asInt());
        assertEquals(20, page0.get("content").size());
        assertEquals(26, page0.get("totalElements").asInt());
        assertEquals(6, GET(s, "/api/v1/products?page=1", 200).get("content").size());

        assertEquals(25, GET(s, "/api/v1/products?keyword=ao mau&size=100", 200).get("totalElements").asInt());
        assertEquals(1, GET(s, "/api/v1/products?keyword=mau-07", 200).get("totalElements").asInt());   // theo ma SKU gan dung
        assertEquals("Quan Jean", GET(s, "/api/v1/products?keyword=" + barcode, 200)
                .get("content").get(0).get("name").asText());                                        // ma vach khop chinh xac
        assertEquals(0, GET(s, "/api/v1/products?keyword=" + barcode.substring(0, 8), 200).get("totalElements").asInt());

        // Ngung ban va loc theo trang thai
        String productId = GET(s, "/api/v1/products?keyword=MAU-01", 200).get("content").get(0).get("id").asText();
        PATCH(s, "/api/v1/products/" + productId + "/status", Map.of("status", "INACTIVE"), 200);
        assertEquals(1, GET(s, "/api/v1/products?status=INACTIVE", 200).get("totalElements").asInt());
        assertEquals(25, GET(s, "/api/v1/products?status=ACTIVE&size=100", 200).get("totalElements").asInt());
    }

    @Test
    void changesPricesWithoutAffectingExistingOrders() {
        Session s = newTenant();
        JsonNode sku = newProduct(s, "Ao Doi Gia", "GIA-1", "100000");
        String skuId = sku.get("id").asText();

        String orderId = POST(s, "/api/v1/orders", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 2))), 201).get("id").asText();
        JsonNode changed = PUT(s, "/api/v1/skus/" + skuId + "/prices", Map.of("retailPrice", "250000", "wholesalePrice", "200000"), 200);
        assertEquals(250000, changed.get("retailPrice").asInt());

        JsonNode order = GET(s, "/api/v1/orders/" + orderId, 200);
        assertEquals(100000, order.get("items").get(0).get("unitPrice").asInt());
        assertEquals(200000, order.get("totalAmount").asInt());

        assertEquals("PR40001", PUT(s, "/api/v1/skus/" + skuId + "/prices", Map.of("retailPrice", "0"), 400)
                .get("errorCode").asText());
    }

    @Test
    void categoryTreeNamesAndDeleteRules() {
        Session s = newTenant();
        String root = POST(s, "/api/v1/categories", Map.of("name", "Thoi trang"), 201).get("id").asText();
        String child = POST(s, "/api/v1/categories", Map.of("name", "Ao", "parentId", root), 201).get("id").asText();

        // Ten trung trong cung cap -> 409; khac cap thi duoc
        assertEquals("PR40901", POST(s, "/api/v1/categories", Map.of("name", "thoi trang"), 409).get("errorCode").asText());
        assertEquals("PR40901", POST(s, "/api/v1/categories", Map.of("name", "AO", "parentId", root), 409).get("errorCode").asText());
        POST(s, "/api/v1/categories", Map.of("name", "Ao"), 201);

        // Khong tao vong lap cha-con
        assertEquals("PR40001", PUT(s, "/api/v1/categories/" + root, Map.of("name", "Thoi trang", "parentId", child), 400)
                .get("errorCode").asText());

        // Khong xoa duoc danh muc con con / con san pham
        assertEquals("PR40902", body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/categories/" + root), s.token(), null, 409)).get("errorCode").asText());
        POST(s, "/api/v1/products", Map.of("name", "Ao Co Loai", "categoryId", child, "hasVariants", false,
                "skus", List.of(sku("CL-1", "1000"))), 201);
        assertEquals("PR40902", body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/categories/" + child), s.token(), null, 409)).get("errorCode").asText());

        // Thuong hieu: khong xoa khi con san pham
        String brand = POST(s, "/api/v1/brands", Map.of("name", "Adidas"), 201).get("id").asText();
        POST(s, "/api/v1/products", Map.of("name", "Ao Co Hieu", "brandId", brand, "hasVariants", false,
                "skus", List.of(sku("CH-1", "1000"))), 201);
        assertEquals("PR40902", body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .delete("/api/v1/brands/" + brand), s.token(), null, 409)).get("errorCode").asText());
        assertEquals("PR40901", POST(s, "/api/v1/brands", Map.of("name", "ADIDAS"), 409).get("errorCode").asText());
    }

    @Test
    void flatSkuSearchReturnsAvailableStockForPos() {
        Session s = newTenant();
        JsonNode a = newProduct(s, "Non Luoi Trai", "NON-A", "70000");
        newProduct(s, "Tui Deo Cheo", "TUI-A", "90000");
        String barcode = POST(s, "/api/v1/skus/" + a.get("id").asText() + "/barcode", null, 200).get("barcode").asText();
        confirmedReceipt(s, a.get("id").asText(), 12, "30000");

        JsonNode byName = GET(s, "/api/v1/skus?keyword=non luoi", 200);
        assertEquals(1, byName.get("totalElements").asInt());
        assertEquals("Non Luoi Trai", byName.get("content").get(0).get("productName").asText());
        assertEquals(12, byName.get("content").get(0).get("available").asInt());

        assertEquals("NON-A", GET(s, "/api/v1/skus?keyword=" + barcode, 200).get("content").get(0).get("code").asText());
        assertEquals(0, GET(s, "/api/v1/skus?keyword=tui", 200).get("content").get(0).get("available").asInt());
        assertEquals(2, GET(s, "/api/v1/skus", 200).get("totalElements").asInt());
    }
}
