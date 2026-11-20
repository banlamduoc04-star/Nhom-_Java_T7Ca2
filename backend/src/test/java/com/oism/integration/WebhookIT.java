package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * FR-SIM-01, FR-ORD-01, NFR-TENANT-01: webhook san xac thuc bang API key cua tenant (header X-Api-Key),
 * tenant lay tu key chu khong tu request; key sai/thieu/sai san -> 401 OD40101.
 */
class WebhookIT extends AbstractIntegrationTest {

    @Test
    void validKeyCreatesReservedOrderForKeyOwnerTenant() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "WHA", 5);
        String key = issueKey(owner, "SHOPEE");

        JsonNode order = webhook("shopee", key, payload(owner.branchId(), "SP-1001", skuId, 2), 201);

        assertEquals("SHOPEE", order.get("channel").asText());
        assertEquals("RESERVED", order.get("status").asText());
        // don thuoc tenant cua key: owner thay, tenant khac nhan 404
        GET(owner, "/api/v1/orders/" + order.get("id").asText(), 200);
        GET(newTenant(), "/api/v1/orders/" + order.get("id").asText(), 404);
    }

    @Test
    void missingWrongOrOtherChannelKeyIsRejectedWithOD40101() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "WHB", 5);
        String shopeeKey = issueKey(owner, "SHOPEE");
        Map<String, Object> body = payload(owner.branchId(), "SP-2001", skuId, 1);

        assertEquals("OD40101", webhook("shopee", null, body, 401).get("errorCode").asText());
        assertEquals("OD40101", webhook("shopee", "oism_khong-ton-tai", body, 401).get("errorCode").asText());
        // key Shopee khong dung duoc cho TikTok; san ngoai ke hoach cung bi tu choi
        assertEquals("OD40101", webhook("tiktok", shopeeKey, body, 401).get("errorCode").asText());
        assertEquals("OD40101", webhook("lazada", shopeeKey, body, 401).get("errorCode").asText());
        // JWT dang nhap khong thay duoc API key
        assertEquals("OD40101", body(call(post("/api/v1/webhooks/shopee"), owner.token(), body, 401))
                .get("errorCode").asText());
        // X-Tenant-Id tu client khong con tac dung
        assertEquals("OD40101", body(call(post("/api/v1/webhooks/shopee").header("X-Tenant-Id", owner.tenantId()),
                null, body, 401)).get("errorCode").asText());
    }

    @Test
    void keyAuthRunsBeforeBodyValidation() {
        // body rong + khong key -> 401 chu khong phai 400
        assertEquals("OD40101", webhook("shopee", null, Map.of(), 401).get("errorCode").asText());
    }

    @Test
    void reissuingKeyRevokesOldKey() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "WHC", 5);
        String oldKey = issueKey(owner, "TIKTOK");
        String newKey = issueKey(owner, "TIKTOK");

        webhook("tiktok", oldKey, payload(owner.branchId(), "TT-1", skuId, 1), 401);
        webhook("tiktok", newKey, payload(owner.branchId(), "TT-1", skuId, 1), 201);

        JsonNode keys = GET(owner, "/api/v1/settings/channel-keys", 200);
        assertEquals(2, keys.size());
        for (JsonNode k : keys) {
            assertFalse(k.has("apiKey"), "Danh sach key khong duoc tra ve key");
            assertEquals("TIKTOK".equals(k.get("channel").asText()), k.get("configured").asBoolean());
        }
    }

    @Test
    void duplicateExternalOrderIsRejectedWithOD40902() {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "WHD", 5);
        String key = issueKey(owner, "SHOPEE");

        webhook("shopee", key, payload(owner.branchId(), "SP-DUP", skuId, 1), 201);
        assertEquals("OD40902", webhook("shopee", key, payload(owner.branchId(), "SP-DUP", skuId, 1), 409)
                .get("errorCode").asText());
    }

    @Test
    void keyOfTenantACannotUseBranchOfTenantB() {
        Session a = newTenant();
        Session b = newTenant();
        String skuB = stockedSku(b, "WHE", 5);
        String keyA = issueKey(a, "SHOPEE");

        // tenant suy ra tu key (A) nen chi nhanh cua B khong hop le voi A
        JsonNode error = webhook("shopee", keyA, payload(b.branchId(), "SP-X", skuB, 1), 400);
        assertEquals("OD40001", error.get("errorCode").asText());
    }

    @Test
    void onlyOwnerManagesKeysAndOnlyWebhookChannels() {
        Session owner = newTenant();
        String staff = newEmployee(owner, "STAFF");

        assertEquals("AU40301", POST(staff, "/api/v1/settings/channel-keys/SHOPEE", null, 403).get("errorCode").asText());
        assertEquals("OD40001", POST(owner, "/api/v1/settings/channel-keys/POS", null, 400).get("errorCode").asText());
        JsonNode issued = POST(owner, "/api/v1/settings/channel-keys/SHOPEE", null, 201);
        assertTrue(issued.get("apiKey").asText().startsWith("oism_"));
    }

    /** San gui lai cung mot don dong thoi: dung 1 don duoc tao, cac lan con lai 409 OD40902, khong co 500. */
    @Test
    void concurrentDuplicateWebhooksCreateOneOrderAndRejectRestWithOD40902() throws Exception {
        Session owner = newTenant();
        String skuId = stockedSku(owner, "WHR", 50);
        String key = issueKey(owner, "TIKTOK");
        byte[] content = json.writeValueAsBytes(payload(owner.branchId(), "TT-RACE", skuId, 1));

        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                go.await();
                return mvc.perform(post("/api/v1/webhooks/tiktok").header("X-Api-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(content)).andReturn();
            }));
        }
        go.countDown();
        int created = 0;
        for (Future<MvcResult> f : futures) {
            MockHttpServletResponse res = f.get(60, TimeUnit.SECONDS).getResponse();
            if (res.getStatus() == 201) {
                created++;
            } else {
                assertEquals(409, res.getStatus(), res.getContentAsString());
                assertEquals("OD40902", body(res.getContentAsString()).get("errorCode").asText());
            }
        }
        pool.shutdown();
        assertEquals(1, created);
        assertEquals(1, GET(owner, "/api/v1/orders?channel=TIKTOK", 200).get("totalElements").asInt());
    }

    private String issueKey(Session owner, String channel) {
        return POST(owner, "/api/v1/settings/channel-keys/" + channel, null, 201).get("apiKey").asText();
    }

    private String stockedSku(Session owner, String code, int qty) {
        String skuCode = code + "-" + UUID.randomUUID().toString().substring(0, 6);
        String skuId = newProduct(owner, "San Pham " + code, skuCode, "100000").get("id").asText();
        confirmedReceipt(owner, skuId, qty, "60000");
        return skuId;
    }

    private static Map<String, Object> payload(String branchId, String externalId, String skuId, int qty) {
        return Map.of("branchId", branchId, "externalOrderId", externalId, "customerName", "Khach San",
                "items", List.of(Map.of("skuId", skuId, "quantity", qty)));
    }

    private JsonNode webhook(String channel, String apiKey, Object payload, int status) {
        MockHttpServletRequestBuilder builder = post("/api/v1/webhooks/" + channel);
        if (apiKey != null) {
            builder.header("X-Api-Key", apiKey);
        }
        return body(call(builder, null, payload, status));
    }
}
