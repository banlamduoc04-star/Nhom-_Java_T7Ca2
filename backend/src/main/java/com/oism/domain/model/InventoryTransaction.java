package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mot dong cua so cai ton kho (FR-INV-01). Chi ghi them: khong setter, @Immutable, va trigger
 * PostgreSQL tu choi UPDATE/DELETE (NFR-SEC-03). Sua sai bang but toan bu.
 */
@Entity
@Immutable
@Table(name = "inventory_transaction")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "branch_id", nullable = false, updatable = false)
    private UUID branchId;

    @Column(name = "sku_id", nullable = false, updatable = false)
    private UUID skuId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 3, updatable = false)
    private StockDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 20, updatable = false)
    private StockReason reason;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "balance_after", nullable = false, updatable = false)
    private int balanceAfter;

    @Column(name = "unit_cost", updatable = false)
    private BigDecimal unitCost;

    @Column(name = "ref_type", updatable = false)
    private String refType;

    @Column(name = "ref_id", updatable = false)
    private UUID refId;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryTransaction() {
    }

    public InventoryTransaction(UUID tenantId, UUID branchId, UUID skuId, StockDirection direction,
                                StockReason reason, int quantity, int balanceAfter, BigDecimal unitCost,
                                String refType, UUID refId, UUID createdBy) {
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.skuId = skuId;
        this.direction = direction;
        this.reason = reason;
        this.quantity = quantity;
        this.balanceAfter = balanceAfter;
        this.unitCost = unitCost;
        this.refType = refType;
        this.refId = refId;
        this.createdBy = createdBy;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getSkuId() {
        return skuId;
    }

    public StockDirection getDirection() {
        return direction;
    }

    public StockReason getReason() {
        return reason;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getBalanceAfter() {
        return balanceAfter;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public String getRefType() {
        return refType;
    }

    public UUID getRefId() {
        return refId;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
