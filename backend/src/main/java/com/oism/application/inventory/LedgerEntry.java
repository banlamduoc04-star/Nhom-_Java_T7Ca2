package com.oism.application.inventory;

import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;

import java.math.BigDecimal;
import java.util.UUID;

/** Mot bien dong ton kho can ghi vao so cai. */
public record LedgerEntry(UUID tenantId, UUID branchId, UUID skuId, StockDirection direction, StockReason reason,
                          int quantity, BigDecimal unitCost, String refType, UUID refId, UUID createdBy) {
}
