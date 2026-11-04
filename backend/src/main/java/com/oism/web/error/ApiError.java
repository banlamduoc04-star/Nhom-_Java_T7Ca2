package com.oism.web.error;

import java.util.List;

/** Dinh dang loi thong nhat cua toan he thong: {errorCode, message, details}. */
public record ApiError(String errorCode, String message, List<FieldDetail> details) {

    public static ApiError of(String errorCode, String message) {
        return new ApiError(errorCode, message, List.of());
    }
}
