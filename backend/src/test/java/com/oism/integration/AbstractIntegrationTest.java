package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final String PASSWORD = "Test@12345";

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> 50);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    /** Tenant moi: tra ve token Owner, tenantId, id chi nhanh mac dinh. */
    protected Session newTenant() {
        String email = "owner-" + UUID.randomUUID() + "@test.vn";
        JsonNode registered = body(call(post("/api/v1/auth/register"), null,
                Map.of("storeName", "Shop " + UUID.randomUUID().toString().substring(0, 6),
                        "fullName", "Chu Shop", "email", email, "password", PASSWORD), 201));
        String token = login(email, PASSWORD);
        return new Session(email, token, registered.get("tenantId").asText(), registered.get("branchId").asText(), registered.get("userId").asText());
    }

    protected String login(String identifier, String password) {
        return body(call(post("/api/v1/auth/login"), null,
                Map.of("identifier", identifier, "password", password), 200)).get("accessToken").asText();
    }

    /** Owner tao nhan vien (STAFF/CASHIER) va tra ve token cua nhan vien do. */
    protected String newEmployee(Session owner, String role) {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@test.vn";
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("email", email);
        req.put("password", PASSWORD);
        req.put("fullName", "Nhan vien " + role);
        req.put("role", role);
        req.put("branchId", owner.branchId());
        call(post("/api/v1/users"), owner.token(), req, 201);
        return login(email, PASSWORD);
    }

    protected String call(MockHttpServletRequestBuilder builder, String token, Object body, int expectedStatus) {
        try {
            builder.contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
            if (body != null) {
                builder.content(json.writeValueAsString(body));
            }
            return mvc.perform(builder).andExpect(status().is(expectedStatus)).andReturn()
                    .getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (AssertionError e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode body(String content) {
        try {
            return json.readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    protected JsonNode POST(Session s, String url, Object body, int status) {
        return body(call(post(url), s.token(), body, status));
    }

    protected JsonNode POST(String token, String url, Object body, int status) {
        return body(call(post(url), token, body, status));
    }

    protected JsonNode GET(Session s, String url, int status) {
        return body(call(get(url), s.token(), null, status));
    }

    protected JsonNode GET(String token, String url, int status) {
        return body(call(get(url), token, null, status));
    }

    protected JsonNode PUT(Session s, String url, Object body, int status) {
        return body(call(put(url), s.token(), body, status));
    }

    protected JsonNode PATCH(Session s, String url, Object body, int status) {
        return body(call(patch(url), s.token(), body, status));
    }

    protected void DELETE(Session s, String url, int status) {
        call(delete(url), s.token(), null, status);
    }

    /**Tao san pham co 1 SKU don gian, tra ve SKU dau tien. */
    protected JsonNode newProduct(Session s, String name, String skuCode, String price) {
        Map<String, Object> sku = new LinkedHashMap<>();
        sku.put("code", skuCode);
        sku.put("retailPrice", price);
        JsonNode product = POST(s, "/api/v1/products", Map.of("name", name, "hasVariants", false, "skus", List.of(sku)), 201);
        return product.get("skus").get(0);
    }

    protected JsonNode confirmedReceipt(Session s, String skuId, int qty, String unitPrice) {
        JsonNode draft = POST(s, "/api/v1/purchase-receipts", Map.of("branchId", s.branchId(),
                "items", List.of(Map.of("skuId", skuId, "quantity", qty, "unitPrice", unitPrice))), 201);
        return POST(s, "/api/v1/purchase-receipts/" + draft.get("id").asText() + "/confirm", null, 200);
    }

    // thêm userId vào record Session để FlashSaleTest có thể sử dụng
    protected record Session(String email, String token, String tenantId, String branchId, String userId) {
    }
}
