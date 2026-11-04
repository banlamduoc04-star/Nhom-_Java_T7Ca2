package com.oism.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** FR-COST-01: 10 vi du tinh tay cua giá vốn bình quân gia quyền (làm tròn 2 chữ số, HALF_UP). */
class CostCalculatorTest {

    @ParameterizedTest(name = "ton {0} x {1} + nhap {2} x {3} = {4}")
    @CsvSource({
            // tonCu, giaVonCu, slNhap, giaNhap, giaVonMoi
            "10, 100000, 30, 120000, 115000.00",   // vi du trong ke hoach
            "0,  0,      50, 80000,  80000.00",    // ton cu bang 0 -> gia von = gia nhap
            "0,  99999,  5,  70000,  70000.00",    // ton cu bang 0 bo qua gia von cu
            "20, 60000,  20, 60000,  60000.00",    // gia nhap bang gia von -> khong doi
            "1,  100,    1,  101,    100.50",
            "3,  10000,  7,  12000,  11400.00",    // (30000 + 84000) / 10
            "7,  15000,  3,  20000,  16500.00",    // (105000 + 60000) / 10
            "5,  33333,  2,  44444,  36507.57",    // (166665 + 88888) / 7 = 36507.5714...
            "100, 1000,  1,  2000,   1009.90",     // (100000 + 2000) / 101 = 1009.9009...
            "2,  1,      1,  2,      1.33",        // (2 + 2) / 3 = 1.3333...
            "4,  2,      4,  3,      2.50",
    })
    void weightedAverage(long oldOnHand, String oldCost, int inQty, String inPrice, String expected) {
        BigDecimal result = CostCalculator.weightedAverage(
                oldOnHand, new BigDecimal(oldCost), inQty, new BigDecimal(inPrice));

        assertEquals(new BigDecimal(expected), result);
    }

    @Test
    void roundsHalfUpToTwoDecimals() {
        // (1 x 1.00 + 1 x 1.01) / 2 = 1.005 -> 1.01
        assertEquals(new BigDecimal("1.01"),
                CostCalculator.weightedAverage(1, new BigDecimal("1.00"), 1, new BigDecimal("1.01")));
    }

    @Test
    void rejectsNonPositiveInQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> CostCalculator.weightedAverage(5, BigDecimal.TEN, 0, BigDecimal.TEN));
    }
}
