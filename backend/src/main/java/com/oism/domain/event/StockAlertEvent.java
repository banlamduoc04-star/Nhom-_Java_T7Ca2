package com.oism.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Su kien canh bao ton kho thap hoac phuc hoi. */
public record StockAlertEvent(
        UUID tenantId,
        UUID alertId,
        UUID branchId,
        UUID skuId,
        String skuCode,
        String status,
        int threshold,
        int availableAtAlert,
        Instant createdAt
) {}
