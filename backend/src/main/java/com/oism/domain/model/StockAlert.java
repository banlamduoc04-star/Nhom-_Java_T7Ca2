package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/**
 * Canh bao ton thap (FR-REP-03; Ke hoach muc 4 bang stock_alert: branch_id, sku_id, available, threshold,
 * status OPEN/CLOSED). Mo khi available <= reorder_point; tu dong dong khi available vuot nguong.
 * Moi (branch, sku) toi da 1 canh bao OPEN (index uq_stock_alert_open).
 */
@Entity
@Table(name = "stock_alert")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StockAlert extends TenantScopedEntity {

    public enum Status {
        OPEN,
        CLOSED
    }

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "sku_id", nullable = false)
    private UUID skuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "threshold", nullable = false)
    private int threshold;

    /** Hang kha dung tai thoi diem mo canh bao. */
    @Column(name = "available", nullable = false)
    private int available;

    @Column(name = "closed_at")
    private Instant closedAt;

    protected StockAlert() {
    }

    public StockAlert(UUID tenantId, UUID branchId, UUID skuId, int threshold, int available) {
        super(tenantId);
        this.branchId = branchId;
        this.skuId = skuId;
        this.threshold = threshold;
        this.available = available;
        this.status = Status.OPEN;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public Status getStatus() {
        return status;
    }

    public int getThreshold() {
        return threshold;
    }

    public int getAvailable() {
        return available;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public boolean isOpen() {
        return status == Status.OPEN;
    }

    public void close() {
        if (status == Status.OPEN) {
            this.status = Status.CLOSED;
            this.closedAt = Instant.now();
        }
    }
}
