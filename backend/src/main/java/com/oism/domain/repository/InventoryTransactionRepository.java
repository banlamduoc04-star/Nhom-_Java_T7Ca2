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

    @Query("""
        SELECT t FROM InventoryTransaction t
        WHERE t.tenantId = :tenantId
          AND (:createdAt IS NULL OR t.createdAt < :createdAt OR (t.createdAt = :createdAt AND t.id < :id))
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<InventoryTransaction> findKeyset(
            @Param("tenantId") UUID tenantId,
            @Param("createdAt") Instant createdAt,
            @Param("id") UUID id,
            Pageable pageable);

    @Query("""
        SELECT t FROM InventoryTransaction t
        WHERE t.tenantId = :tenantId
          AND t.branchId = :branchId
          AND (:createdAt IS NULL OR t.createdAt < :createdAt OR (t.createdAt = :createdAt AND t.id < :id))
        ORDER BY t.createdAt DESC, t.id DESC
    """)
    List<InventoryTransaction> findKeysetByBranch(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("createdAt") Instant createdAt,
            @Param("id") UUID id,
            Pageable pageable);
}
