package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * NFR-SEC-02 (ACID): loi giua chuoi Tao don -> Giu -> Tru khong de lai du lieu do dang.
 * Gia lap loi o tung buoc: Giu (dong hang thu 2 thieu hang), Tru (CSDL tu choi ghi so cai SALE) va
 * ghi thanh toan (CSDL tu choi ghi payment). Loi CSDL tao bang trigger tam, chi nham vao SKU/tenant cua test.
 * Sau loi: khong co don, khong co thanh toan, reserved/on_hand va so cai giu nguyen.
 */
class TransactionRollbackIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void dropFailureTriggers() {
        jdbc.execute("DROP TRIGGER IF EXISTS trg_test_fail_sale ON inventory_transaction");
        jdbc.execute("DROP TRIGGER IF EXISTS trg_test_fail_payment ON payment");
        jdbc.execute("DROP FUNCTION IF EXISTS test_fail_insert()");
    }

    /** Trigger tam: moi INSERT thoa dieu kien deu loi, giong su co CSDL giua giao dich. */
    private void failInsertsWhen(String table, String trigger, String condition) {
        jdbc.execute("CREATE OR REPLACE FUNCTION test_fail_insert() RETURNS trigger AS $$ "
                + "BEGIN RAISE EXCEPTION 'gia lap loi CSDL (test NFR-SEC-02)'; END; $$ LANGUAGE plpgsql");
        jdbc.execute("CREATE TRIGGER " + trigger + " BEFORE INSERT ON " + table
                + " FOR EACH ROW WHEN (" + condition + ") EXECUTE FUNCTION test_fail_insert()");
    }

    @Test
    void posCheckoutFailingAtReserveStepLeavesNothingBehind() {
        Session owner = newTenant();
        String skuA = stockedSku(owner, 5);
        String skuB = stockedSku(owner, 1);

        JsonNode error = POST(owner, "/api/v1/pos/checkout", checkout(owner.branchId(),
                List.of(line(skuA, 2), line(skuB, 3))), 409);

        assertEquals("IV40901", error.get("errorCode").asText());
        assertUntouched(owner, skuA, 5);
        assertUntouched(owner, skuB, 1);
    }

    @Test
    void posCheckoutFailingAtDeductStepRollsBackReservationOrderAndPayment() {
        Session owner = newTenant();
        String skuA = stockedSku(owner, 5);
        String skuB = stockedSku(owner, 5);
        // Buoc Tru: CSDL tu choi but toan SALE cua skuB (sau khi da tao don, giu hang, tru xong skuA)
        failInsertsWhen("inventory_transaction", "trg_test_fail_sale",
                "NEW.reason = 'SALE' AND NEW.sku_id = '" + skuB + "'");

        JsonNode error = POST(owner, "/api/v1/pos/checkout", checkout(owner.branchId(),
                List.of(line(skuA, 1), line(skuB, 2))), 500);

        assertEquals("SY50001", error.get("errorCode").asText());
        assertUntouched(owner, skuA, 5);
        assertUntouched(owner, skuB, 5);
    }

    @Test
    void posCheckoutFailingAtPaymentStepRollsBackEverything() {
        Session owner = newTenant();
        String skuA = stockedSku(owner, 5);
        // Buoc cuoi: CSDL tu choi ghi payment (don da Completed, kho da tru trong cung giao dich)
        failInsertsWhen("payment", "trg_test_fail_payment", "NEW.tenant_id = '" + owner.tenantId() + "'");

        JsonNode error = POST(owner, "/api/v1/pos/checkout", checkout(owner.branchId(), List.of(line(skuA, 2))), 500);

        assertEquals("SY50001", error.get("errorCode").asText());
        assertUntouched(owner, skuA, 5);
    }

    @Test
    void marketplaceOrderFailingAtReserveStepIsNotCreated() {
        Session owner = newTenant();
        String skuA = stockedSku(owner, 5);
        String skuB = stockedSku(owner, 1);
        String key = POST(owner, "/api/v1/settings/channel-keys/SHOPEE", null, 201).get("apiKey").asText();

        JsonNode error = body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/webhooks/shopee").header("X-Api-Key", key), null,
                Map.of("branchId", owner.branchId(), "externalOrderId", "SP-ACID-1",
                        "items", List.of(line(skuA, 2), line(skuB, 3))), 409));

        assertEquals("IV40901", error.get("errorCode").asText());
        assertUntouched(owner, skuA, 5);
        assertUntouched(owner, skuB, 1);
        // gui lai dung ma don ngoai sau khi het loi van tao duoc (khong con "don ma")
        body(call(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/webhooks/shopee").header("X-Api-Key", key), null,
                Map.of("branchId", owner.branchId(), "externalOrderId", "SP-ACID-1",
                        "items", List.of(line(skuA, 2))), 201));
    }

    /** Khong co don/thanh toan nao, ton giu nguyen, so cai chi co dong PURCHASE cua phieu nhap. */
    private void assertUntouched(Session owner, String skuId, int onHand) {
        assertEquals(0, GET(owner, "/api/v1/orders", 200).get("totalElements").asInt(), "Khong duoc con don do dang");
        JsonNode stock = GET(owner, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(onHand, stock.get("onHand").asInt());
        assertEquals(0, stock.get("reserved").asInt(), "reserved phai ve 0");
        JsonNode ledger = GET(owner, "/api/v1/inventory-transactions?skuId=" + skuId, 200).get("content");
        assertEquals(1, ledger.size(), "So cai chi co but toan nhap kho");
        assertEquals("PURCHASE", ledger.get(0).get("reason").asText());
    }

    private String stockedSku(Session owner, int qty) {
        String skuId = newProduct(owner, "Ao Acid", "ACID-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, qty, "60000");
        return skuId;
    }

    private static Map<String, Object> line(String skuId, int qty) {
        return Map.of("skuId", skuId, "quantity", qty);
    }

    private static Map<String, Object> checkout(String branchId, List<Map<String, Object>> items) {
        return Map.of("branchId", branchId, "items", items, "paymentMethod", "CASH", "cashReceived", 10_000_000);
    }
}
