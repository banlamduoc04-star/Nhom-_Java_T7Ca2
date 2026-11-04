package com.oism.application.inventory;

import java.math.BigDecimal;
import java.util.UUID;

/** Dong phieu nhap: dau vao cua tao/sua phieu (skuId, quantity, unitPrice). */
public record ReceiptLine(UUID skuId, int quantity, BigDecimal unitPrice) {
}
