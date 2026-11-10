package com.oism.application.inventory;

import com.oism.domain.model.StockAlert;

import java.time.Instant;
import java.util.UUID;

public record StockAlertView(
        UUID id,
        UUID branchId,
        String branchName,
        UUID skuId,
        String skuCode,
        String productName,
        StockAlert.Status status,
        int threshold,
        int available,
        Instant createdAt,
        Instant closedAt
) {}

