package com.oism.domain.repository;

import com.oism.domain.model.InventoryTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Chi doc va them moi; khong khai bao ham sua/xoa nao cho so cai. */
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, UUID>, JpaSpecificationExecutor<InventoryTransaction> {

    boolean existsByTenantIdAndSkuId(UUID tenantId, UUID skuId);

    // Keyset (NFR-TENANT-02): khong dung "tham so IS NULL" vi PostgreSQL khong suy ra kieu cua tham so null
    // ("could not determine data type of parameter") -> tach rieng trang dau / trang sau, co / khong loc chi nhanh.

    List<InventoryTransaction> findByTenantIdOrderByCreatedAtDescIdDesc(UUID tenantId, Pageable pageable);

    List<InventoryTransaction> findByTenantIdAndBranchIdOrderByCreatedAtDescIdDesc(
            UUID tenantId, UUID branchId, Pageable pageable);

    /** Trang sau: (created_at, id) < con tro, dung tron index ix_invtx_tenant_created_id. */
    @Query("""
        SELECT t FROM InventoryTransaction t
        WHERE t.tenantId = :tenantId
          AND (t.createdAt, t.id) < (:createdAt, :id)
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<InventoryTransaction> findKeysetAfter(
            @Param("tenantId") UUID tenantId,
            @Param("createdAt") Instant createdAt,
            @Param("id") UUID id,
            Pageable pageable);

    @Query("""
        SELECT t FROM InventoryTransaction t
        WHERE t.tenantId = :tenantId
          AND t.branchId = :branchId
          AND (t.createdAt, t.id) < (:createdAt, :id)
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<InventoryTransaction> findKeysetAfterByBranch(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("createdAt") Instant createdAt,
            @Param("id") UUID id,
            Pageable pageable);
}
