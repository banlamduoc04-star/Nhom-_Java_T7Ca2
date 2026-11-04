package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.UUID;

/** Dong hang cua don: unit_price chup tai thoi diem ban (FR-PROD-04); cost_price ghi khi Confirmed (FR-COST-02). */
@Entity
@Table(name = "order_item")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class OrderItem extends TenantScopedEntity {

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "cost_price")
    private BigDecimal costPrice;

    protected OrderItem() {
    }

    public OrderItem(UUID tenantId, UUID orderId, UUID skuId, int quantity, BigDecimal unitPrice) {
        super(tenantId);
        this.orderId = orderId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }
}
