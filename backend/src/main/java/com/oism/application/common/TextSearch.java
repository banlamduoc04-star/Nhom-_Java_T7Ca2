package com.oism.application.common;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

import java.text.Normalizer;

/**
 * FR-PROD-02/05: chuan hoa ten (NFC) va so khop gan dung khong phan biet hoa/thuong, khong phan biet dau
 * ("ao thun" khop "Áo Thun"). Dung ham f_unaccent tao o migration V13.
 */
public final class TextSearch {

    private TextSearch() {
    }

    /** Cat khoang trang hai dau va chuan hoa Unicode NFC, de "Á" go kieu to hop va dung san luu giong nhau. */
    public static String normalizeName(String name) {
        return Normalizer.normalize(name.trim(), Normalizer.Form.NFC);
    }

    /** f_unaccent(lower(column)) LIKE f_unaccent(lower('%keyword%')). */
    public static Predicate likeIgnoreAccent(CriteriaBuilder cb, Expression<String> column, String keyword) {
        String like = "%" + Normalizer.normalize(keyword.trim(), Normalizer.Form.NFC).toLowerCase() + "%";
        return cb.like(fold(cb, column), fold(cb, cb.literal(like)));
    }

    private static Expression<String> fold(CriteriaBuilder cb, Expression<String> e) {
        return cb.function("f_unaccent", String.class, cb.lower(e));
    }
}
