package com.oism.domain.service;

import com.oism.infrastructure.entity.InventoryEntity;
import com.oism.domain.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("NFR-TENANT-02: Kiểm thử 100 luồng trừ kho đồng thời - Không bị âm kho nhờ Redisson & Optimistic Lock")
    void testConcurrentDeductions_DistributedLock_NoNegativeStock() throws InterruptedException {
        String tenantId = "TENANT_TEST";
        String skuCode = "SKU-IPHONE-15";
        
        // 1. Khởi tạo kho ban đầu có 50 sản phẩm
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

        // 2. Cho 100 luồng cùng tranh chấp trừ kho (mỗi luồng trừ 1 sản phẩm)
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    inventoryService.deductStock(tenantId, skuCode, 1L);
                } catch (Exception e) {
                    // Cố ý nuốt exception do các luồng vượt quá tồn kho (khi kho về 0)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // Chờ tất cả 100 luồng chạy xong

        // 3. Kiểm tra số lượng tồn kho còn lại phải bằng 0 (Không được bị âm)
        InventoryEntity updatedInventory = inventoryRepository.findByTenantIdAndSkuCode(tenantId, skuCode).orElseThrow();
        assertEquals(0L, updatedInventory.getQuantity(), "Kho phải về đúng 0 và không bao giờ bị âm!");
    }
}