package com.oism.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** NFR-TENANT-02 / FR-INV-01: phan trang keyset so cai (trang dau truoc day loi 500 do tham so null khong kieu). */
class LedgerKeysetIT extends AbstractIntegrationTest {

    @Test
    void keysetPagesWithoutGapsOrDuplicatesAndStaysInTenant() {
        Session owner = newTenant();
        String skuId = newProduct(owner, "Ao Keyset", "KS-" + UUID.randomUUID().toString().substring(0, 8),
                "100000").get("id").asText();
        for (int i = 0; i < 5; i++) {
            confirmedReceipt(owner, skuId, i + 1, "60000");
        }

        Set<String> seen = new HashSet<>();
        String url = "/api/v1/inventory-transactions/keyset?limit=2";
        JsonNode page = GET(owner, url, 200);
        int pages = 0;
        String prevCreatedAt = null;
        while (page.size() > 0) {
            pages++;
            for (JsonNode row : page) {
                assertTrue(seen.add(row.get("id").asText()), "Khong duoc lap dong giua cac trang");
                if (prevCreatedAt != null) {
                    assertTrue(row.get("createdAt").asText().compareTo(prevCreatedAt) <= 0, "Thu tu moi -> cu");
                }
                prevCreatedAt = row.get("createdAt").asText();
            }
            JsonNode last = page.get(page.size() - 1);
            page = GET(owner, url + "&lastCreatedAt=" + last.get("createdAt").asText()
                    + "&lastId=" + last.get("id").asText(), 200);
        }
        assertEquals(5, seen.size(), "Du 5 but toan nhap kho");
        assertEquals(3, pages);

        assertEquals(5, GET(owner, "/api/v1/inventory-transactions/keyset?limit=10&branchId=" + owner.branchId(), 200).size());
        assertEquals("IV40001", GET(owner, "/api/v1/inventory-transactions/keyset?lastId=" + UUID.randomUUID(), 400)
                .get("errorCode").asText());
        assertEquals(0, GET(newTenant(), "/api/v1/inventory-transactions/keyset?limit=10", 200).size());
        assertEquals("AU40301", GET(newEmployee(owner, "CASHIER"), "/api/v1/inventory-transactions/keyset", 403)
                .get("errorCode").asText());
    }
}
