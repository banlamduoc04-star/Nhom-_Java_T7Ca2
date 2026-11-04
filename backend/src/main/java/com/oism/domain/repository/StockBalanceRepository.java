package com.oism.domain.repository;

import com.oism.domain.model.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockBalanceRepository extends JpaRepository<StockBalance, UUID>, JpaSpecificationExecutor<StockBalance> {

    /** Khoa pessimistic dong ton (SELECT ... FOR UPDATE) - moi thao tac ghi so cai phai di qua day. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.tenantId = :tenantId and b.branchId = :branchId and b.skuId = :skuId")
    Optional<StockBalance> findForUpdate(@Param("tenantId") UUID tenantId,
                                         @Param("branchId") UUID branchId,
                                         @Param("skuId") UUID skuId);

    /** Tao dong ton 0 neu chua co; an toan khi nhieu luong cung tao (ON CONFLICT DO NOTHING). */
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into stock_balance (id, tenant_id, branch_id, sku_id, on_hand, reserved, created_at, updated_at) "
            + "values (gen_random_uuid(), :tenantId, :branchId, :skuId, 0, 0, now(), now()) "
            + "on conflict (tenant_id, branch_id, sku_id) do nothing", nativeQuery = true)
    void insertIfAbsent(@Param("tenantId") UUID tenantId,
                        @Param("branchId") UUID branchId,
                        @Param("skuId") UUID skuId);

    /** Tong on_hand cua mot SKU tren moi chi nhanh (ton cu de tinh gia von - FR-COST-01). */
    @Query("select coalesce(sum(b.onHand), 0) from StockBalance b where b.tenantId = :tenantId and b.skuId = :skuId")
    long sumOnHand(@Param("tenantId") UUID tenantId, @Param("skuId") UUID skuId);

    /** Hang kha dung cua mot SKU (tong tat ca chi nhanh hoac mot chi nhanh). */
    @Query("select coalesce(sum(b.onHand - b.reserved), 0) from StockBalance b "
            + "where b.tenantId = :tenantId and b.skuId = :skuId and (:branchId is null or b.branchId = :branchId)")
    long sumAvailable(@Param("tenantId") UUID tenantId, @Param("skuId") UUID skuId, @Param("branchId") UUID branchId);

    boolean existsByTenantIdAndBranchIdAndOnHandGreaterThan(UUID tenantId, UUID branchId, int onHand);

    List<StockBalance> findByTenantIdAndSkuIdIn(UUID tenantId, Collection<UUID> skuIds);
}
