package com.oism.application.order;

import com.oism.domain.model.OrderChannel;

import java.util.UUID;

/** Ben goi webhook da xac thuc bang API key: tenant va san suy ra tu key, khong lay tu request (FR-SIM-01). */
public record WebhookCaller(UUID tenantId, OrderChannel channel) {
}
