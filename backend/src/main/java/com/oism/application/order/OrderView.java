package com.oism.application.order;

import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderView(UUID id, String code, UUID branchId, OrderChannel channel, String externalOrderId,
                        OrderStatus status, String customerName, String customerPhone, String shippingAddress,
                        BigDecimal totalAmount, Instant reservedUntil, Instant createdAt,
                        List<Item> items, List<History> history) {

    public record Item(UUID skuId, String skuCode, int quantity, BigDecimal unitPrice, BigDecimal costPrice,
                       BigDecimal lineTotal) {
    }

    public record History(OrderStatus fromStatus, OrderStatus toStatus, UUID changedBy, String reason,
                          Instant createdAt) {
    }
}
