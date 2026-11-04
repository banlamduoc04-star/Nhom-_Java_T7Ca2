package com.oism.domain.service;

/** Ma vach EAN-13 (FR-PROD-03): 12 chu so + 1 chu so kiem tra (check digit). */
public final class Ean13 {

    private Ean13() {
    }

    /** Tinh check digit cho 12 chu so dau. */
    public static int checkDigit(String first12) {
        if (first12 == null || !first12.matches("\\d{12}")) {
            throw new IllegalArgumentException("EAN-13 can dung 12 chu so de tinh check digit");
        }
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = first12.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return (10 - sum % 10) % 10;
    }

    /** Ghep 12 chu so dau voi check digit thanh ma 13 chu so. */
    public static String complete(String first12) {
        return first12 + checkDigit(first12);
    }

    public static boolean isValid(String code) {
        return code != null && code.matches("\\d{13}") && checkDigit(code.substring(0, 12)) == code.charAt(12) - '0';
    }
}
