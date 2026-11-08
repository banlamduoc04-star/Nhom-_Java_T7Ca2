package com.oism.domain.service;

import com.dev.app.infrastructure.entity.InventoryEntity;
import com.dev.app.infrastructure.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * Service Quản lý tồn kho: Xử lý trừ kho an toàn, khóa đồng bộ Redis (Bug-INV-01).
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final RedissonClient redissonClient;
    private final CostCalculator costCalculator;

    @Transactional
    public void deductStock(String tenantId, String skuCode, long quantity) {
        String lockKey = "lock:inventory:" + tenantId + ":" + skuCode;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    InventoryEntity inventory = inventoryRepository.findByTenantIdAndSkuCode(tenantId, skuCode)
                            .orElseThrow(() -> new IllegalArgumentException("SKU không tồn tại trong kho"));

                    if (inventory.getQuantity() < quantity) {
                        throw new IllegalStateException("Số lượng tồn kho không đủ (Tồn: " + inventory.getQuantity() + ")");
                    }

                    inventory.setQuantity(inventory.getQuantity() - quantity);
                    inventoryRepository.save(inventory);
                } finally {
                    lock.unlock();
                }
            } else {
                throw new RuntimeException("Hệ thống bận, vui lòng thử lại sau.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thao tác bị ngắt kết nối", e);
        }
    }

    @Transactional
    public void addStock(String tenantId, String skuCode, long incomingQty, BigDecimal unitPrice) {
        InventoryEntity inventory = inventoryRepository.findByTenantIdAndSkuCode(tenantId, skuCode)
                .orElse(InventoryEntity.builder()
                        .tenantId(tenantId)
                        .skuCode(skuCode)
                        .quantity(0L)
                        .averageUnitCost(BigDecimal.ZERO)
                        .build());

        BigDecimal currentQtyBD = BigDecimal.valueOf(inventory.getQuantity());
        BigDecimal currentTotalVal = currentQtyBD.multiply(inventory.getAverageUnitCost());

        BigDecimal newAverageCost = costCalculator.calculateMovingAverageCost(
                currentQtyBD, currentTotalVal, BigDecimal.valueOf(incomingQty), unitPrice
        );

        inventory.setQuantity(inventory.getQuantity() + incomingQty);
        inventory.setAverageUnitCost(newAverageCost);

        inventoryRepository.save(inventory);
    }
}
