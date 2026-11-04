package com.oism.domain.model;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.UUID;

/** Phieu nhap kho (FR-INV-02): Nhap -> Da xac nhan. Phieu da xac nhan khong sua duoc (IV40902). */
@Entity
@Table(name = "purchase_receipt")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class PurchaseReceipt extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "supplier_id")
    private UUID supplierId;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReceiptStatus status = ReceiptStatus.DRAFT;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by")
    private UUID confirmedBy;

    @Column(name = "created_by")
    private UUID createdBy;

    protected PurchaseReceipt() {
    }

    public PurchaseReceipt(UUID tenantId, UUID branchId, UUID supplierId, String code, UUID createdBy) {
        super(tenantId);
        this.branchId = branchId;
        this.supplierId = supplierId;
        this.code = code;
        this.createdBy = createdBy;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public String getCode() {
        return code;
    }

    public ReceiptStatus getStatus() {
        return status;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public UUID getConfirmedBy() {
        return confirmedBy;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public boolean isDraft() {
        return status == ReceiptStatus.DRAFT;
    }

    public void assertEditable() {
        if (!isDraft()) {
            throw new BusinessException(ErrorCode.IV40902);
        }
    }

    /** Doi chi nhanh / nha cung cap khi con o trang thai Nhap. */
    public void update(UUID branchId, UUID supplierId) {
        assertEditable();
        this.branchId = branchId;
        this.supplierId = supplierId;
    }

    public void markConfirmed(UUID userId) {
        assertEditable();
        this.status = ReceiptStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.confirmedBy = userId;
    }
}
