package com.oism.domain.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

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

    public static BigDecimal calculateMovingAverageCost(BigDecimal currentQty, BigDecimal currentTotalVal, BigDecimal incomingQty, BigDecimal unitPrice) {
        if (incomingQty == null || incomingQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số lượng nhập phải lớn hơn 0");
        }
        if (currentQty == null || currentQty.compareTo(BigDecimal.ZERO) <= 0) {
            return unitPrice.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal incomingTotal = incomingQty.multiply(unitPrice);
        BigDecimal newTotalQty = currentQty.add(incomingQty);
        return currentTotalVal.add(incomingTotal).divide(newTotalQty, 4, RoundingMode.HALF_UP);
    }
}
