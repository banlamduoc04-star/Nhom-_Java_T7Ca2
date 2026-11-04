package com.oism.realtime;

import java.time.Instant;

public record OrderEvent(
        String orderId,
        String eventType,
        Instant occurredAt
) {
    public static OrderEvent created(String orderId) {
        return new OrderEvent(
                orderId,
                "ORDER_CREATED",
                Instant.now()
        );
    }
}