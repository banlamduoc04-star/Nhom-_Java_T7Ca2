package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FR-SIM-02 + nghiem thu Sprint 4 ("gui webhook tu Simulator thi Admin hien popup"):
 * client STOMP that (cong ngau nhien) nhan don moi va canh bao ton cua tenant minh sau commit;
 * khong token thi bi tu choi, tenant khac khong nghe duoc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeIT extends AbstractIntegrationTest {

    @LocalServerPort
    int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUpClient() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(json);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void stopClient() {
        stompClient.stop();
    }

    @Test
    void webhookOrderAndLowStockAlertArePushedToOwnTenantOnly() throws Exception {
        Session owner = newTenant();
        Session other = newTenant();
        String skuId = skuWithReorderPoint(owner, 5);
        confirmedReceipt(owner, skuId, 20, "100000");
        String key = POST(owner, "/api/v1/settings/channel-keys/SHOPEE", null, 201).get("apiKey").asText();

        StompSession ownerSession = connect(owner.token(), new LinkedBlockingQueue<>());
        BlockingQueue<Map<String, Object>> alerts = subscribe(ownerSession, "/topic/tenants/" + owner.tenantId() + "/alerts");
        BlockingQueue<Map<String, Object>> orders = subscribe(ownerSession, "/topic/tenants/" + owner.tenantId() + "/orders");

        // tenant khac nghe kenh cua owner -> server tra frame ERROR, khong nhan duoc tin
        BlockingQueue<String> otherErrors = new LinkedBlockingQueue<>();
        StompSession otherSession = connect(other.token(), otherErrors);
        BlockingQueue<Map<String, Object>> leaked = subscribe(otherSession, "/topic/tenants/" + owner.tenantId() + "/orders");
        assertNotNull(otherErrors.poll(10, TimeUnit.SECONDS), "SUBSCRIBE kenh tenant khac phai bi tu choi");

        // SUBSCRIBE xu ly bat dong bo: gui don (moi don giu 1) den khi owner nhan duoc thong bao
        Map<String, Object> pushedOrder = null;
        String lastCode = null;
        int reserved = 0;
        for (int i = 0; i < 5 && pushedOrder == null; i++) {
            lastCode = webhook(key, owner.branchId(), skuId, "SP-RT-" + i, 1).get("code").asText();
            reserved++;
            pushedOrder = orders.poll(2, TimeUnit.SECONDS);
        }
        assertNotNull(pushedOrder, "Owner phai nhan thong bao don moi tu webhook");
        assertEquals("SHOPEE", pushedOrder.get("channel"));
        assertEquals(true, pushedOrder.get("orderCode").toString().startsWith("DH-"));
        assertNotNull(lastCode);

        // giu them de available = 5 = nguong -> canh bao OPEN duoc day ve kenh alerts
        webhook(key, owner.branchId(), skuId, "SP-RT-LOW", 20 - reserved - 5);
        Map<String, Object> pushedAlert = null;
        for (int i = 0; i < 10 && pushedAlert == null; i++) {
            pushedAlert = alerts.poll(1, TimeUnit.SECONDS);
        }
        assertNotNull(pushedAlert, "available 5 <= 5: phai co canh bao ton");
        assertEquals("OPEN", pushedAlert.get("status"));
        assertEquals(5, ((Number) pushedAlert.get("available")).intValue());

        assertNull(leaked.poll(500, TimeUnit.MILLISECONDS), "Tenant khac khong duoc nhan don cua owner");
    }

    @Test
    void connectWithoutValidTokenIsRejected() {
        assertThrows(ExecutionException.class, () -> connect(null, new LinkedBlockingQueue<>()));
        assertThrows(ExecutionException.class, () -> connect("khong-phai-jwt", new LinkedBlockingQueue<>()));
    }

    private JsonNode webhook(String key, String branchId, String skuId, String externalId, int qty) {
        return body(call(post("/api/v1/webhooks/shopee").header("X-Api-Key", key), null,
                Map.of("branchId", branchId, "externalOrderId", externalId,
                        "items", List.of(Map.of("skuId", skuId, "quantity", qty))), 201));
    }

    private StompSession connect(String token, BlockingQueue<String> errors) throws Exception {
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        return stompClient.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connectHeaders, new StompSessionHandlerAdapter() {
                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errors.add("ERROR " + headers.getFirst("message"));
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        errors.add("TRANSPORT " + exception.getMessage());
                    }
                }).get(5, TimeUnit.SECONDS);
    }

    private BlockingQueue<Map<String, Object>> subscribe(StompSession session, String destination) {
        BlockingQueue<Map<String, Object>> queue = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders h) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders h, Object payload) {
                queue.add((Map<String, Object>) payload);
            }
        });
        return queue;
    }

    private String skuWithReorderPoint(Session owner, int reorderPoint) {
        Map<String, Object> sku = new LinkedHashMap<>();
        sku.put("code", "RT-" + UUID.randomUUID().toString().substring(0, 8));
        sku.put("retailPrice", "150000");
        sku.put("reorderPoint", reorderPoint);
        return POST(owner, "/api/v1/products", Map.of("name", "Ao Realtime", "hasVariants", false,
                "skus", List.of(sku)), 201).get("skus").get(0).get("id").asText();
    }
}
