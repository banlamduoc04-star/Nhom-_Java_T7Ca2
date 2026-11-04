package com.oism.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Ean13Test {

    @ParameterizedTest
    @CsvSource({
            "400638133393, 1",   // 4006381333931 (vi du chuan cua GS1)
            "590123412345, 7",   // 5901234123457
            "893600111222, 5",
            "000000000000, 0",
    })
    void checkDigit(String first12, int expected) {
        assertEquals(expected, Ean13.checkDigit(first12));
        assertTrue(Ean13.isValid(Ean13.complete(first12)));
    }

    @Test
    void rejectsWrongCheckDigitAndBadFormat() {
        assertFalse(Ean13.isValid("4006381333930"));
        assertFalse(Ean13.isValid("123"));
        assertFalse(Ean13.isValid(null));
        assertFalse(Ean13.isValid("40063813339ab"));
    }

    @Test
    void checkDigitNeedsExactly12Digits() {
        assertThrows(IllegalArgumentException.class, () -> Ean13.checkDigit("12345"));
    }
}
