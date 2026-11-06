package com.oism.application.order;

import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PosCheckoutResponse(
        UUID orderId,
        String orderCode,
        OrderStatus status,
        UUID branchId,
        String branchName,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        BigDecimal cashReceived,
        BigDecimal changeAmount,
        Instant completedAt,
        String cashierName,
        List<PosReceiptLine> items
) {
    public record PosReceiptLine(
            UUID skuId,
            String skuCode,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {}
}

