package com.oism.domain.model;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/** Phien kiem ke kho tai mot chi nhanh (FR-INV-04). Bang stocktake. */
@Entity
@Table(name = "stocktake")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class Stocktake extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StocktakeStatus status = StocktakeStatus.OPEN;

    @Column(name = "approved_by")
    private UUID approvedBy;

    protected Stocktake() {
    }

    public Stocktake(UUID tenantId, UUID branchId, String code) {
        super(tenantId);
        this.branchId = branchId;
        this.code = code;
        this.status = StocktakeStatus.OPEN;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public String getCode() {
        return code;
    }

    public StocktakeStatus getStatus() {
        return status;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void markApproved(UUID userId) {
        if (this.status != StocktakeStatus.OPEN) {
            throw new BusinessException(ErrorCode.IV40902, "Phiên kiểm kê đã được duyệt trước đó");
        }
        this.status = StocktakeStatus.APPROVED;
        this.approvedBy = userId;
    }

    public void assertOpen() {
        if (this.status != StocktakeStatus.OPEN) {
            throw new BusinessException(ErrorCode.IV40902, "Phiên kiểm kê đã duyệt, không thể chỉnh sửa");
        }
    }
}
