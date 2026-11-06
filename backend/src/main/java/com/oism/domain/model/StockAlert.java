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
 * Canh bao ton thap (FR-REP-03, JC-30): khi available <= reorder_point thi tao canh bao OPEN.
 * Tu dong chuyen RESOLVED khi nhap hang vuot nguong.
 */
@Entity
@Table(name = "stock_alert")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StockAlert extends TenantScopedEntity {

    public enum Status {
        OPEN,
        RESOLVED
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

    @Column(name = "available_at_alert", nullable = false)
    private int availableAtAlert;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected StockAlert() {
    }

    public StockAlert(UUID tenantId, UUID branchId, UUID skuId, int threshold, int availableAtAlert) {
        super(tenantId);
        this.branchId = branchId;
        this.skuId = skuId;
        this.threshold = threshold;
        this.availableAtAlert = availableAtAlert;
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

    public int getAvailableAtAlert() {
        return availableAtAlert;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void resolve() {
        this.status = Status.RESOLVED;
        this.resolvedAt = Instant.now();
    }
}

