package com.oism.domain.service;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.oism.domain.model.OrderStatus.CANCELLED;
import static com.oism.domain.model.OrderStatus.COMPLETED;
import static com.oism.domain.model.OrderStatus.CONFIRMED;
import static com.oism.domain.model.OrderStatus.DRAFT;
import static com.oism.domain.model.OrderStatus.RESERVED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** FR-ORD-02: du cac buoc hop le va moi buoc bi chan, ke ca khi da o trang thai cuoi. */
class OrderStateMachineTest {

    @ParameterizedTest(name = "{0} -> {1} hop le")
    @CsvSource({
            "DRAFT,     RESERVED",
            "DRAFT,     CANCELLED",
            "RESERVED,  CONFIRMED",
            "RESERVED,  CANCELLED",
            "CONFIRMED, COMPLETED",
            "CONFIRMED, CANCELLED",
    })
    void allowedTransitions(OrderStatus from, OrderStatus to) {
        assertTrue(OrderStateMachine.canTransition(from, to));
        assertDoesNotThrow(() -> OrderStateMachine.assertTransition(from, to));
    }

    @Test
    void newOrderMayStartAsDraftOrReservedOnly() {
        assertTrue(OrderStateMachine.canTransition(null, DRAFT));
        assertTrue(OrderStateMachine.canTransition(null, RESERVED));
        assertFalse(OrderStateMachine.canTransition(null, CONFIRMED));
        assertFalse(OrderStateMachine.canTransition(null, COMPLETED));
        assertFalse(OrderStateMachine.canTransition(null, CANCELLED));
    }

    @ParameterizedTest(name = "{0} -> {1} bi chan")
    @CsvSource({
            "DRAFT,     CONFIRMED",
            "DRAFT,     COMPLETED",
            "DRAFT,     DRAFT",
            "RESERVED,  DRAFT",
            "RESERVED,  COMPLETED",
            "CONFIRMED, DRAFT",
            "CONFIRMED, RESERVED",
            "COMPLETED, CANCELLED",
            "COMPLETED, DRAFT",
            "CANCELLED, DRAFT",
            "CANCELLED, RESERVED",
            "CANCELLED, CONFIRMED",
    })
    void blockedTransitionsThrowOD40901(OrderStatus from, OrderStatus to) {
        assertFalse(OrderStateMachine.canTransition(from, to));
        BusinessException ex = assertThrows(BusinessException.class, () -> OrderStateMachine.assertTransition(from, to));
        assertEquals(ErrorCode.OD40901, ex.getErrorCode());
    }

    @Test
    void completedAndCancelledAreFinal() {
        for (OrderStatus s : OrderStatus.values()) {
            if (s.isFinal()) {
                assertTrue(OrderStateMachine.allowedFrom(s).isEmpty(), s + " la trang thai cuoi");
            }
        }
    }
}
