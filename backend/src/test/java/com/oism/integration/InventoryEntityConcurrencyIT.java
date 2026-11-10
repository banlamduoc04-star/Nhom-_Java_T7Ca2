package com.oism.integration;

import com.oism.application.inventory.InventoryService;
import com.oism.domain.model.InventoryEntity;
import com.oism.domain.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryEntityConcurrencyIT extends AbstractIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("NFR-TENANT-02: Kiểm thử 100 luồng trừ kho đồng thời - Không bị âm kho")
    void testConcurrentDeductions_DistributedLock_NoNegativeStock() throws InterruptedException {
        String tenantId = "TENANT_TEST";
        String skuCode = "SKU-IPHONE-15";

        inventoryRepository.save(InventoryEntity.builder()
                .tenantId(tenantId)
                .skuCode(skuCode)
                .quantity(50L)
                .averageUnitCost(new BigDecimal("1000.0000"))
                .version(0L)
                .build());

        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    inventoryService.deductStock(tenantId, skuCode, 1L);
                } catch (Exception e) {
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        InventoryEntity updatedInventory = inventoryRepository.findByTenantIdAndSkuCode(tenantId, skuCode).orElseThrow();
        assertEquals(0L, updatedInventory.getQuantity(), "Kho phải về đúng 0 và không bao giờ bị âm!");
    }
}
