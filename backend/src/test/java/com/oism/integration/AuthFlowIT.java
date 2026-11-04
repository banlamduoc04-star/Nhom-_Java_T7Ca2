package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** FR-AUTH-01/02, NFR-SEC-01 qua HTTP that: dang ky, dang nhap, lam moi xoay vong, dang xuat, /me. */
class AuthFlowIT extends AbstractIntegrationTest {

    private JsonNode loginFull(String identifier, String password, int status) {
        return body(call(post("/api/v1/auth/login"), null, Map.of("identifier", identifier, "password", password), status));
    }

    @Test
    void registerLoginMeRefreshAndLogout() {
        Session s = newTenant();
        JsonNode tokens = loginFull(s.email(), PASSWORD, 200);
        assertEquals("Bearer", tokens.get("tokenType").asText());
        assertEquals(3600, tokens.get("accessExpiresIn").asInt());
        assertEquals(7 * 24 * 3600, tokens.get("refreshExpiresIn").asInt());

        JsonNode me = body(call(get("/api/v1/me"), tokens.get("accessToken").asText(), null, 200));
        assertEquals("OWNER", me.get("role").asText());
        assertEquals(s.tenantId(), me.get("tenantId").asText());

        // Refresh xoay vong: token moi khac token cu
        String oldRefresh = tokens.get("refreshToken").asText();
        JsonNode renewed = body(call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", oldRefresh), 200));
        assertNotEquals(oldRefresh, renewed.get("refreshToken").asText());

        // Dung lai refresh cu -> 401 AU40101 va thu hoi luon token moi (nghi bi danh cap)
        assertEquals("AU40101", body(call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", oldRefresh), 401))
                .get("errorCode").asText());
        call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", renewed.get("refreshToken").asText()), 401);

        // Dang xuat thu hoi refresh token (idempotent)
        JsonNode fresh = loginFull(s.email(), PASSWORD, 200);
        call(post("/api/v1/auth/logout"), fresh.get("accessToken").asText(), Map.of("refreshToken", fresh.get("refreshToken").asText()), 204);
        call(post("/api/v1/auth/logout"), fresh.get("accessToken").asText(), Map.of("refreshToken", fresh.get("refreshToken").asText()), 204);
        call(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", fresh.get("refreshToken").asText()), 401);
    }

    @Test
    void rejectsBadCredentialsDuplicateEmailAndInvalidInput() {
        Session s = newTenant();

        assertEquals("AU40101", loginFull(s.email(), "sai-mat-khau", 401).get("errorCode").asText());
        assertEquals("AU40101", loginFull("khong-ton-tai@test.vn", PASSWORD, 401).get("errorCode").asText());
        assertEquals("AU40901", body(call(post("/api/v1/auth/register"), null, Map.of("storeName", "Shop Moi",
                "fullName", "Chu", "email", s.email().toUpperCase(), "password", PASSWORD), 409)).get("errorCode").asText());

        JsonNode invalid = body(call(post("/api/v1/auth/register"), null, Map.of("storeName", "", "fullName", "Chu",
                "email", "khong-phai-email", "password", "123"), 400));
        assertEquals("AU40001", invalid.get("errorCode").asText());
        assertEquals(3, invalid.get("details").size());

        assertEquals("AU40101", body(call(get("/api/v1/me"), "token.sai.hoan.toan", null, 401)).get("errorCode").asText());
    }
}
