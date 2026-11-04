package com.oism.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gia von binh quan gia quyen (FR-COST-01), tinh theo SKU tren toan tenant:
 * (ton cu x gia von cu + SL nhap x gia nhap) / (ton cu + SL nhap), lam tron 2 chu so (HALF_UP).
 * Ton cu bang 0 thi gia von moi = gia nhap.
 */
public final class CostCalculator {

    private static final int SCALE = 2;

    private CostCalculator() {
    }

    public static BigDecimal weightedAverage(long oldOnHand, BigDecimal oldCost, int inQuantity, BigDecimal inPrice) {
        if (inQuantity <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        if (oldOnHand <= 0) {
            return inPrice.setScale(SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal oldValue = oldCost.multiply(BigDecimal.valueOf(oldOnHand));
        BigDecimal inValue = inPrice.multiply(BigDecimal.valueOf(inQuantity));
        BigDecimal totalQty = BigDecimal.valueOf(oldOnHand).add(BigDecimal.valueOf(inQuantity));
        return oldValue.add(inValue).divide(totalQty, SCALE, RoundingMode.HALF_UP);
    }
}
