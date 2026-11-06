package com.oism.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Su kien don hang moi duoc tao thanh cong. */
public record OrderCreatedEvent(
        UUID tenantId,
        UUID orderId,
        String orderCode,
        BigDecimal totalAmount,
        String channel,
        String createdBy,
        Instant createdAt
) {}

