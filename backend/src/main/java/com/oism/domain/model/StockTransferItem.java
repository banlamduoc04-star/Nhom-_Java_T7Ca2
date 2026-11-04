package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Dong hang trong phieu chuyen kho (FR-INV-03). Bang stock_transfer_item. */
@Entity
@Table(name = "stock_transfer_item")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StockTransferItem extends TenantScopedEntity {

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected StockTransferItem() {
    }

    public StockTransferItem(UUID tenantId, UUID transferId, UUID skuId, int quantity) {
        super(tenantId);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Số lượng chuyển phải lớn hơn 0");
        }
        this.transferId = transferId;
        this.skuId = skuId;
        this.quantity = quantity;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public int getQuantity() {
        return quantity;
    }
}
