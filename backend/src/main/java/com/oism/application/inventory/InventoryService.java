package com.oism.application.inventory;

import com.oism.domain.model.InventoryEntity;
import com.oism.domain.repository.InventoryRepository;
import com.oism.domain.service.CostCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void deductStock(String tenantId, String skuCode, long quantity) {
        String lockKey = tenantId + ":" + skuCode;
        ReentrantLock lock = locks.computeIfAbsent(lockKey, k -> new ReentrantLock());

        try {
            if (lock.tryLock(5, TimeUnit.SECONDS)) {
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

        BigDecimal newAverageCost = CostCalculator.calculateMovingAverageCost(
                currentQtyBD, currentTotalVal, BigDecimal.valueOf(incomingQty), unitPrice
        );

        inventory.setQuantity(inventory.getQuantity() + incomingQty);
        inventory.setAverageUnitCost(newAverageCost);

        inventoryRepository.save(inventory);
    }
}
