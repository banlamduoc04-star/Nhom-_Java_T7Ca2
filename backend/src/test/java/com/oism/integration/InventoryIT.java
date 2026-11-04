package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.oism.application.inventory.InventoryLedgerService;
import com.oism.application.inventory.LedgerEntry;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FR-INV-01/02/05, FR-COST-01, NFR-SEC-03: nghiem thu Sprint 2 (nhap kho, so cai, gia von). */
class InventoryIT extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    InventoryLedgerService ledger;

    @Autowired
    TransactionTemplate tx;

    @Test
    void sprint2Acceptance_twoReceiptsGiveWeightedAverageCostAndLedgerRows() {
        Session s = newTenant();
        // San pham 3 bien the
        JsonNode product = POST(s, "/api/v1/products", Map.of("name", "Ao Ba Bien The", "hasVariants", true, "skus", List.of(
                Map.of("code", "BT-S", "retailPrice", "200000"),
                Map.of("code", "BT-M", "retailPrice", "200000"),
                Map.of("code", "BT-L", "retailPrice", "200000"))), 201);
        String skuId = product.get("skus").get(0).get("id").asText();

        // Phieu 1: 10 cai x 100.000; phieu 2: 30 cai x 120.000 -> (10x100.000 + 30x120.000) / 40 = 115.000
        confirmedReceipt(s, skuId, 10, "100000");
        confirmedReceipt(s, skuId, 30, "120000");

        JsonNode sku = null;
        for (JsonNode candidate : GET(s, "/api/v1/products/" + product.get("id").asText(), 200).get("skus")) {
            if (candidate.get("id").asText().equals(skuId)) {
                sku = candidate;
            }
        }
        assertEquals(0, new BigDecimal("115000.00").compareTo(sku.get("avgCost").decimalValue()));

        JsonNode stock = GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(40, stock.get("onHand").asInt());
        assertEquals(0, stock.get("reserved").asInt());
        assertEquals(40, stock.get("available").asInt());
        assertEquals("BT-S", stock.get("skuCode").asText());

        // So cai: 2 dong IN, balance_after dung (moi nhat truoc)
        JsonNode rows = GET(s, "/api/v1/inventory-transactions?skuId=" + skuId, 200).get("content");
        assertEquals(2, rows.size());
        assertEquals(40, rows.get(0).get("balanceAfter").asInt());
        assertEquals(10, rows.get(1).get("balanceAfter").asInt());
        assertEquals("IN", rows.get(0).get("direction").asText());
        assertEquals("PURCHASE", rows.get(0).get("reason").asText());
        assertEquals("PURCHASE_RECEIPT", rows.get(0).get("refType").asText());
    }

    @Test
    void ledgerRejectsUpdateAndDeleteAtDatabaseLevel() {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao So Cai", "LED-1", "100000").get("id").asText();
        confirmedReceipt(s, skuId, 5, "60000");

        DataAccessException update = assertThrows(DataAccessException.class,
                () -> jdbc.update("update inventory_transaction set quantity = 999 where tenant_id = ?::uuid", s.tenantId()));
        assertTrue(update.getMostSpecificCause().getMessage().contains("chi duoc ghi them"));
        assertThrows(DataAccessException.class,
                () -> jdbc.update("delete from inventory_transaction where tenant_id = ?::uuid", s.tenantId()));

        assertEquals(1, jdbc.queryForObject(
                "select count(*) from inventory_transaction where tenant_id = ?::uuid", Integer.class, s.tenantId()));
    }

    @Test
    void confirmedReceiptCannotBeChangedOrConfirmedAgain() {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Phieu Nhap", "PN-1", "100000").get("id").asText();
        JsonNode confirmed = confirmedReceipt(s, skuId, 4, "50000");
        String id = confirmed.get("id").asText();

        assertEquals("CONFIRMED", confirmed.get("status").asText());
        Map<String, Object> edit = Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 99, "unitPrice", "1")));
        assertEquals("IV40902", PUT(s, "/api/v1/purchase-receipts/" + id, edit, 409).get("errorCode").asText());
        assertEquals("IV40902", POST(s, "/api/v1/purchase-receipts/" + id + "/confirm", null, 409).get("errorCode").asText());

        // Chi xac nhan mot lan -> ton chi tang 4
        assertEquals(4, GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0).get("onHand").asInt());
    }

    @Test
    void draftReceiptIsEditableAndWritesNothingUntilConfirmed() {
        Session s = newTenant();
        String sku1 = newProduct(s, "Ao Nhap Mot", "DR-1", "100000").get("id").asText();
        String sku2 = newProduct(s, "Ao Nhap Hai", "DR-2", "100000").get("id").asText();

        JsonNode draft = POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", sku1, "quantity", 2, "unitPrice", "10000"))), 201);
        String id = draft.get("id").asText();
        assertEquals("DRAFT", draft.get("status").asText());
        assertEquals(0, GET(s, "/api/v1/stock", 200).get("totalElements").asInt());
        assertEquals(0, GET(s, "/api/v1/inventory-transactions", 200).get("totalElements").asInt());

        JsonNode edited = PUT(s, "/api/v1/purchase-receipts/" + id, Map.of("branchId", s.branchId(), "items", List.of(
                Map.of("skuId", sku1, "quantity", 3, "unitPrice", "10000"),
                Map.of("skuId", sku2, "quantity", 5, "unitPrice", "20000"))), 200);
        assertEquals(2, edited.get("items").size());
        assertEquals(0, new BigDecimal("130000").compareTo(edited.get("totalAmount").decimalValue()));

        POST(s, "/api/v1/purchase-receipts/" + id + "/confirm", null, 200);
        assertEquals(2, GET(s, "/api/v1/inventory-transactions", 200).get("totalElements").asInt());
    }

    @Test
    void validatesReceiptInputAndSuppliers() {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Kiem Tra", "VAL-1", "100000").get("id").asText();

        assertEquals("IV40001", POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 0, "unitPrice", "1"))), 400).get("errorCode").asText());
        assertEquals("IV40001", POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1, "unitPrice", "-1"))), 400).get("errorCode").asText());
        assertEquals("IV40001", POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of()), 400).get("errorCode").asText());
        assertEquals("IV40001", POST(s, "/api/v1/purchase-receipts", Map.of("branchId", UUID.randomUUID(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1, "unitPrice", "1"))), 400).get("errorCode").asText());
        assertEquals("PR40401", POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", UUID.randomUUID(), "quantity", 1, "unitPrice", "1"))), 404)
                .get("errorCode").asText());
        assertEquals("IV40401", GET(s, "/api/v1/purchase-receipts/" + UUID.randomUUID(), 404).get("errorCode").asText());

        JsonNode supplier = POST(s, "/api/v1/suppliers", Map.of("name", "NCC Mot", "phone", "0281234567"), 201);
        JsonNode stopped = PUT(s, "/api/v1/suppliers/" + supplier.get("id").asText(),
                Map.of("name", "NCC Mot", "active", false), 200);
        assertEquals(false, stopped.get("active").asBoolean());
        assertEquals(1, GET(s, "/api/v1/suppliers", 200).size());
        POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(), "supplierId", supplier.get("id").asText(),
                "items", List.of(Map.of("skuId", skuId, "quantity", 1, "unitPrice", "1"))), 201);
    }

    /** 50 luong cung xuat 1 cai tu SKU con dung 1 cai: dung 1 thanh cong, 49 bi IV40901, ton khong am. */
    @Test
    void concurrentOutNeverOversells() throws Exception {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Flash Sale", "FLASH-1", "100000").get("id").asText();
        confirmedReceipt(s, skuId, 1, "50000");
        UUID tenantId = UUID.fromString(s.tenantId());
        UUID branchId = UUID.fromString(s.branchId());

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Void> task = () -> {
                ready.countDown();
                go.await();
                try {
                    tx.executeWithoutResult(status -> ledger.post(new LedgerEntry(tenantId, branchId,
                            UUID.fromString(skuId), StockDirection.OUT, StockReason.SALE, 1, null, "TEST", null, null)));
                    ok.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.IV40901) {
                        insufficient.incrementAndGet();
                    } else {
                        throw e;
                    }
                }
                return null;
            };
            futures.add(pool.submit(task));
        }
        ready.await();
        go.countDown();
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(1, ok.get());
        assertEquals(49, insufficient.get());
        JsonNode stock = GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0);
        assertEquals(0, stock.get("onHand").asInt());
        assertEquals(0, stock.get("available").asInt());
        // 1 dong IN (nhap) + 1 dong OUT (ban)
        assertEquals(2, GET(s, "/api/v1/inventory-transactions?skuId=" + skuId, 200).get("totalElements").asInt());
    }

    @Test
    void concurrentReceiptsOnSameSkuKeepAverageCostConsistent() throws Exception {
        Session s = newTenant();
        String skuId = newProduct(s, "Ao Nhap Song Song", "PAR-1", "100000").get("id").asText();
        int threads = 8;
        List<String> receiptIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            receiptIds.add(POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                    "items", List.of(Map.of("skuId", skuId, "quantity", 10, "unitPrice", "100000"))), 201).get("id").asText());
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (String id : receiptIds) {
            futures.add(pool.submit(() -> POST(s, "/api/v1/purchase-receipts/" + id + "/confirm", null, 200)));
        }
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();

        assertEquals(80, GET(s, "/api/v1/stock?skuId=" + skuId, 200).get("content").get(0).get("onHand").asInt());
        JsonNode sku = GET(s, "/api/v1/products?keyword=PAR-1", 200).get("content").get(0);
        JsonNode detail = GET(s, "/api/v1/products/" + sku.get("id").asText(), 200).get("skus").get(0);
        assertEquals(0, new BigDecimal("100000.00").compareTo(detail.get("avgCost").decimalValue()));
    }
}
