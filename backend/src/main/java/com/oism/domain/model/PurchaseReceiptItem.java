package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.util.UUID;

/** Dong cua phieu nhap: so luong > 0, don gia >= 0. */
@Entity
@Table(name = "purchase_receipt_item")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class PurchaseReceiptItem extends TenantScopedEntity {

    @Column(name = "receipt_id", nullable = false)
    private UUID receiptId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    protected PurchaseReceiptItem() {
    }

    public PurchaseReceiptItem(UUID tenantId, UUID receiptId, UUID skuId, int quantity, BigDecimal unitPrice) {
        super(tenantId);
        this.receiptId = receiptId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public UUID getReceiptId() {
        return receiptId;
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
}
