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

/** Phieu chuyen kho 2 buoc giua 2 chi nhanh (FR-INV-03). Bang stock_transfer. */
@Entity
@Table(name = "stock_transfer")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class StockTransfer extends TenantScopedEntity {

    @Column(name = "from_branch_id", nullable = false)
    private UUID fromBranchId;

    @Column(name = "to_branch_id", nullable = false)
    private UUID toBranchId;

    @Column(name = "code", nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status = TransferStatus.DRAFT;

    protected StockTransfer() {
    }

    public StockTransfer(UUID tenantId, UUID fromBranchId, UUID toBranchId, String code) {
        super(tenantId);
        if (fromBranchId.equals(toBranchId)) {
            throw new BusinessException(ErrorCode.IV40001, "Chi nhánh xuất và chi nhánh nhận không được trùng nhau");
        }
        this.fromBranchId = fromBranchId;
        this.toBranchId = toBranchId;
        this.code = code;
        this.status = TransferStatus.DRAFT;
    }

    public UUID getFromBranchId() {
        return fromBranchId;
    }

    public UUID getToBranchId() {
        return toBranchId;
    }

    public String getCode() {
        return code;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void markInTransit() {
        if (this.status != TransferStatus.DRAFT) {
            throw new BusinessException(ErrorCode.IV40902, "Chỉ xuất kho được phiếu ở trạng thái Nháp (DRAFT)");
        }
        this.status = TransferStatus.IN_TRANSIT;
    }

    public void markReceived() {
        if (this.status != TransferStatus.IN_TRANSIT) {
            throw new BusinessException(ErrorCode.IV40902, "Chỉ nhận kho được phiếu đang vận chuyển (IN_TRANSIT)");
        }
        this.status = TransferStatus.RECEIVED;
    }

    public void markCancelled() {
        if (this.status != TransferStatus.DRAFT) {
            throw new BusinessException(ErrorCode.IV40902, "Chỉ hủy được phiếu ở trạng thái Nháp (DRAFT)");
        }
        this.status = TransferStatus.CANCELLED;
    }
}
