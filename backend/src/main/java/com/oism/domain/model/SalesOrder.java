package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Don hang chuan hoa cho moi kenh (FR-ORD-01). Bang "orders" (tranh tu khoa ORDER cua SQL). */
@Entity(name = "SalesOrder")
@Table(name = "orders")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class SalesOrder extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private OrderChannel channel;

    @Column(name = "external_order_id")
    private String externalOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "reserved_until")
    private Instant reservedUntil;

    @Column(name = "created_by")
    private UUID createdBy;

    protected SalesOrder() {
    }

    public SalesOrder(UUID tenantId, UUID branchId, String code, OrderChannel channel, String externalOrderId,
                      OrderStatus status, String customerName, String customerPhone, String shippingAddress,
                      BigDecimal totalAmount, UUID createdBy) {
        super(tenantId);
        this.branchId = branchId;
        this.code = code;
        this.channel = channel;
        this.externalOrderId = externalOrderId;
        this.status = status;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.shippingAddress = shippingAddress;
        this.totalAmount = totalAmount;
        this.createdBy = createdBy;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getCode() {
        return code;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public String getExternalOrderId() {
        return externalOrderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public Instant getReservedUntil() {
        return reservedUntil;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    /** Chi duoc goi sau khi OrderStateMachine da chap nhan buoc chuyen. */
    public void moveTo(OrderStatus next) {
        this.status = next;
    }

    public void setReservedUntil(Instant reservedUntil) {
        this.reservedUntil = reservedUntil;
    }
}
