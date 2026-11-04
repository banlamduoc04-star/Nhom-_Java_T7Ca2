package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.UUID;

/** Thanh toan cua don (dung tu Sprint 4 cho POS). */
@Entity
@Table(name = "payment")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class Payment extends TenantScopedEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 10)
    private PaymentMethod method;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "cash_received")
    private BigDecimal cashReceived;

    @Column(name = "change_amount")
    private BigDecimal changeAmount;

    protected Payment() {
    }

    public Payment(UUID tenantId, UUID orderId, PaymentMethod method, BigDecimal amount,
                   BigDecimal cashReceived, BigDecimal changeAmount) {
        super(tenantId);
        this.orderId = orderId;
        this.method = method;
        this.amount = amount;
        this.cashReceived = cashReceived;
        this.changeAmount = changeAmount;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getCashReceived() {
        return cashReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }
}
