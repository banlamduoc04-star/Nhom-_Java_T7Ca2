package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * Ton kho theo SKU x chi nhanh (FR-RSE-01). on_hand chi duoc doi qua InventoryLedgerService
 * trong cung giao dich voi dong so cai. available = on_hand - reserved, khong bao gio am.
 */
@Entity
@Table(name = "stock_balance")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StockBalance extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Column(name = "on_hand", nullable = false)
    private int onHand;

    @Column(name = "reserved", nullable = false)
    private int reserved;

    protected StockBalance() {
    }

    public StockBalance(UUID tenantId, UUID branchId, UUID skuId, int onHand, int reserved) {
        super(tenantId);
        this.branchId = branchId;
        this.skuId = skuId;
        this.onHand = onHand;
        this.reserved = reserved;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public int getOnHand() {
        return onHand;
    }

    public int getReserved() {
        return reserved;
    }

    public int getAvailable() {
        return onHand - reserved;
    }

    /** Chi goi tu InventoryLedgerService. */
    public void applyOnHand(int newOnHand) {
        this.onHand = newOnHand;
    }

    /** Chi goi tu ReservationService de giu hoac nha hang giu (FR-RSE-01..03). */
    public void applyReserved(int newReserved) {
        if (newReserved < 0) {
            throw new IllegalArgumentException("Hàng giữ không được âm");
        }
        if (newReserved > onHand) {
            throw new IllegalArgumentException("Hàng giữ không được vượt tồn thực tế");
        }
        this.reserved = newReserved;
    }
}
