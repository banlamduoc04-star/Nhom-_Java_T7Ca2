package com.oism.application.inventory;

import com.oism.domain.model.ReceiptStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReceiptView(UUID id, String code, UUID branchId, UUID supplierId, ReceiptStatus status,
                          Instant confirmedAt, Instant createdAt, BigDecimal totalAmount, List<Item> items) {

    public record Item(UUID skuId, String skuCode, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
