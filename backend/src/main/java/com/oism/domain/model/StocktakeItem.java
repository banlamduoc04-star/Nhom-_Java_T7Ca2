package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Dong kiem ke cua tung SKU (FR-INV-04). Bang stocktake_item. */
@Entity
@Table(name = "stocktake_item")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StocktakeItem extends TenantScopedEntity {

    @Column(name = "stocktake_id", nullable = false)
    private UUID stocktakeId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "system_qty", nullable = false)
    private int systemQty;

    @Column(name = "counted_qty")
    private Integer countedQty;

    @Column(name = "difference")
    private Integer difference;

    protected StocktakeItem() {
    }

    public StocktakeItem(UUID tenantId, UUID stocktakeId, UUID skuId, int systemQty) {
        super(tenantId);
        this.stocktakeId = stocktakeId;
        this.skuId = skuId;
        this.systemQty = systemQty;
    }

    public UUID getStocktakeId() {
        return stocktakeId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public int getSystemQty() {
        return systemQty;
    }

    public Integer getCountedQty() {
        return countedQty;
    }

    public Integer getDifference() {
        return difference;
    }

    public void updateCountedQty(int countedQty) {
        this.countedQty = countedQty;
        this.difference = countedQty - systemQty;
    }
}
