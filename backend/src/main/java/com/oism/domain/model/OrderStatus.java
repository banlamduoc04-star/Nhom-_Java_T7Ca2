package com.oism.domain.model;

/** Trang thai don hang (Ke hoach muc 4 - Chuyen trang thai don hang). Completed va Cancelled la trang thai cuoi. */
public enum OrderStatus {
    DRAFT,
    RESERVED,
    CONFIRMED,
    COMPLETED,
    CANCELLED;

    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
