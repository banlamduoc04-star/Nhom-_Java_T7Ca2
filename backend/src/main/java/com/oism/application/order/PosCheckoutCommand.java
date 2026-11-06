package com.oism.application.order;

import com.oism.domain.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PosCheckoutCommand(
        UUID branchId,
        List<PosLine> lines,
        PaymentMethod paymentMethod,
        BigDecimal cashReceived,
        String customerName,
        String customerPhone,
        String note
) {
    public record PosLine(UUID skuId, int quantity) {}
}

