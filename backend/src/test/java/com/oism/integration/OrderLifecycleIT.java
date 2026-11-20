package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.oism.application.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Bang chung tu dong cho test case Sprint 3: TC-ORD-05/06 (huy don giu / da xac nhan), TC-RSE-03 (khong deadlock
 * khi khoa SKU nguoc thu tu), TC-RSE-04 (tu huy don giu qua han), TC-TRN-04 (huy phieu chuyen nhap).
 */
class OrderLifecycleIT extends AbstractIntegrationTest {

    @Autowired
    OrderService orderService;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void cancelReservedReleasesAndCancelConfirmedReturnsStockWithLedger() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, 10);

        // TC-ORD-05: huy don dang giu -> nha reserved
        String reserved = draft(owner, skuId, 3);
        POST(owner, "/api/v1/orders/" + reserved + "/reserve", null, 200);
        assertStock(owner, skuId, 10, 3);
        POST(owner, "/api/v1/orders/" + reserved + "/cancel", Map.of("reason", "Khach huy"), 200);
        assertStock(owner, skuId, 10, 0);

        // TC-ORD-06: huy don da xac nhan -> tang on_hand, ghi so cai IN/RETURN voi gia von da chup
        String confirmed = draft(owner, skuId, 4);
        POST(owner, "/api/v1/orders/" + confirmed + "/reserve", null, 200);
        POST(owner, "/api/v1/orders/" + confirmed + "/confirm", null, 200);
        assertStock(owner, skuId, 6, 0);
        POST(owner, "/api/v1/orders/" + confirmed + "/cancel", Map.of("reason", "Hoan truoc khi giao"), 200);
        assertStock(owner, skuId, 10, 0);
        JsonNode last = GET(owner, "/api/v1/inventory-transactions?skuId=" + skuId, 200).get("content").get(0);
        assertEquals("IN", last.get("direction").asText());
        assertEquals("RETURN", last.get("reason").asText());
        assertEquals(4, last.get("quantity").asInt());
        assertEquals(0, new java.math.BigDecimal("60000").compareTo(last.get("unitCost").decimalValue()));
    }

    @Test
    void concurrentOrdersLockingSkusInOppositeOrderNeverDeadlock() throws Exception {
        Session owner = newTenant();
        String skuA = stockedSku(owner, 500);
        String skuB = stockedSku(owner, 500);
        String key = POST(owner, "/api/v1/settings/channel-keys/SHOPEE", null, 201).get("apiKey").asText();

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int n = i;
            // nua so luong gui A truoc B, nua con lai B truoc A
            List<Map<String, Object>> items = n % 2 == 0
                    ? List.of(Map.of("skuId", skuA, "quantity", 1), Map.of("skuId", skuB, "quantity", 1))
                    : List.of(Map.of("skuId", skuB, "quantity", 1), Map.of("skuId", skuA, "quantity", 1));
            Callable<Integer> task = () -> {
                go.await();
                return mvc.perform(post("/api/v1/webhooks/shopee").header("X-Api-Key", key)
                                .contentType("application/json")
                                .content(json.writeValueAsString(Map.of("branchId", owner.branchId(),
                                        "externalOrderId", "DL-" + n + "-" + UUID.randomUUID(), "items", items))))
                        .andReturn().getResponse().getStatus();
            };
            results.add(pool.submit(task));
        }
        go.countDown();
        for (Future<Integer> f : results) {
            assertEquals(201, f.get(60, TimeUnit.SECONDS), "Moi don deu phai giu hang thanh cong, khong deadlock/500");
        }
        pool.shutdown();
        assertStock(owner, skuA, 500, threads);
        assertStock(owner, skuB, 500, threads);
    }

    @Test
    void expiredReservationIsCancelledAutomaticallyAndReleased() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, 5);
        String key = POST(owner, "/api/v1/settings/channel-keys/TIKTOK", null, 201).get("apiKey").asText();
        String orderId = body(call(post("/api/v1/webhooks/tiktok").header("X-Api-Key", key), null,
                Map.of("branchId", owner.branchId(), "externalOrderId", "TT-EXP-1",
                        "items", List.of(Map.of("skuId", skuId, "quantity", 2))), 201)).get("id").asText();
        assertStock(owner, skuId, 5, 2);

        // Gia lap da qua reserved_until roi chay dung ham tac vu nen goi moi phut (FR-SIM-03)
        jdbc.update("UPDATE orders SET reserved_until = now() - interval '1 minute' WHERE id = ?::uuid", orderId);
        int cancelled = orderService.cancelExpiredReservations(UUID.fromString(owner.tenantId()));

        assertEquals(1, cancelled);
        JsonNode order = GET(owner, "/api/v1/orders/" + orderId, 200);
        assertEquals("CANCELLED", order.get("status").asText());
        JsonNode lastHistory = order.get("history").get(order.get("history").size() - 1);
        assertTrue(lastHistory.get("reason").asText().contains("Quá hạn"));
        assertStock(owner, skuId, 5, 0);
    }

    @Test
    void draftTransferCanBeCancelledButDispatchedCannot() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, 5);
        String warehouse = POST(owner, "/api/v1/branches", Map.of("name", "Kho " + UUID.randomUUID().toString().substring(0, 4),
                "type", "WAREHOUSE"), 201).get("id").asText();
        Map<String, Object> req = Map.of("fromBranchId", owner.branchId(), "toBranchId", warehouse,
                "items", List.of(Map.of("skuId", skuId, "quantity", 1)));

        String draft = POST(owner, "/api/v1/transfers", req, 201).get("id").asText();
        assertEquals("CANCELLED", POST(owner, "/api/v1/transfers/" + draft + "/cancel", null, 200).get("status").asText());
        assertStock(owner, skuId, 5, 0);

        String dispatched = POST(owner, "/api/v1/transfers", req, 201).get("id").asText();
        POST(owner, "/api/v1/transfers/" + dispatched + "/dispatch", null, 200);
        assertEquals("IV40902", POST(owner, "/api/v1/transfers/" + dispatched + "/cancel", null, 409).get("errorCode").asText());
    }

    private String draft(Session owner, String skuId, int qty) {
        return POST(owner, "/api/v1/orders", Map.of("branchId", owner.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", qty))), 201).get("id").asText();
    }

    private String stockedSku(Session owner, int qty) {
        String skuId = newProduct(owner, "Ao Vong Doi", "LC-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        confirmedReceipt(owner, skuId, qty, "60000");
        return skuId;
    }

    private void assertStock(Session owner, String skuId, int onHand, int reserved) {
        JsonNode s = GET(owner, "/api/v1/stock?skuId=" + skuId + "&branchId=" + owner.branchId(), 200).get("content").get(0);
        assertEquals(onHand, s.get("onHand").asInt(), "on_hand");
        assertEquals(reserved, s.get("reserved").asInt(), "reserved");
    }
}
