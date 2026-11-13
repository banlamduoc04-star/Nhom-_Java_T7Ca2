package com.oism.application.order;

import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Du lieu in phieu giao hang (FR-ORD-03; GET /orders/{id}/shipping-slip). */
public record ShippingSlipView(
        UUID orderId,
        String orderCode,
        OrderChannel channel,
        String externalOrderId,
        OrderStatus status,
        String branchName,
        String branchAddress,
        String customerName,
        String customerPhone,
        String shippingAddress,
        Instant createdAt,
        BigDecimal totalAmount,
        List<Line> lines) {

    public record Line(String skuCode, String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }
}
