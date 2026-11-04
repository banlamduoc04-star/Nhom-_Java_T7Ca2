package com.oism.application.inventory;

import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionView(UUID id, UUID branchId, UUID skuId, String skuCode, StockDirection direction,
                              StockReason reason, int quantity, int balanceAfter, BigDecimal unitCost,
                              String refType, UUID refId, UUID createdBy, Instant createdAt) {
}
