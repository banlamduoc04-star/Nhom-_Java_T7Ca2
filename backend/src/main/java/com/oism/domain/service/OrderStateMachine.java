package com.oism.domain.service;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * May trang thai don hang (FR-ORD-02), thuan domain, khong phu thuoc Spring.
 * Buoc hop le: (moi) -> Draft | Reserved; Draft -> Reserved | Cancelled; Reserved -> Confirmed | Cancelled;
 * Confirmed -> Completed | Cancelled. Buoc khac tra OD40901.
 */
public final class OrderStateMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> NEXT = new EnumMap<>(OrderStatus.class);
    private static final Set<OrderStatus> INITIAL = EnumSet.of(OrderStatus.DRAFT, OrderStatus.RESERVED);

    static {
        NEXT.put(OrderStatus.DRAFT, EnumSet.of(OrderStatus.RESERVED, OrderStatus.CANCELLED));
        NEXT.put(OrderStatus.RESERVED, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        NEXT.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        NEXT.put(OrderStatus.COMPLETED, EnumSet.noneOf(OrderStatus.class));
        NEXT.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
    }

    private OrderStateMachine() {
    }

    /** Trang thai dau tien khi tao don (from = null). */
    public static boolean canStartAt(OrderStatus first) {
        return INITIAL.contains(first);
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        if (from == null) {
            return canStartAt(to);
        }
        return NEXT.get(from).contains(to);
    }

    public static Set<OrderStatus> allowedFrom(OrderStatus from) {
        return from == null ? EnumSet.copyOf(INITIAL) : EnumSet.copyOf(NEXT.get(from));
    }

    public static void assertTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new BusinessException(ErrorCode.OD40901,
                    "Không thể chuyển đơn từ " + (from == null ? "(mới)" : from) + " sang " + to);
        }
    }
}
