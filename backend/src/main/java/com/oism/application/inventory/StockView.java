package com.oism.application.inventory;

import java.util.UUID;

/** Ton kho cua mot SKU tai mot chi nhanh: available = on_hand - reserved. */
public record StockView(UUID branchId, UUID skuId, String skuCode, int onHand, int reserved, int available) {
}
