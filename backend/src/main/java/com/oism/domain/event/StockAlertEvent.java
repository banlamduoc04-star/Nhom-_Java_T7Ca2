package com.oism.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Su kien canh bao ton kho thap (status OPEN) hoac da dong (CLOSED), phat sau commit (FR-REP-03, FR-SIM-02). */
public record StockAlertEvent(
        UUID tenantId,
        UUID alertId,
        UUID branchId,
        UUID skuId,
        String skuCode,
        String status,
        int threshold,
        int available,
        Instant createdAt
) {}

