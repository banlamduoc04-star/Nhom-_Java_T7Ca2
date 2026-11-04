package com.oism.domain.repository;

import com.oism.domain.model.Sku;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkuRepository extends JpaRepository<Sku, UUID>, JpaSpecificationExecutor<Sku> {

    Optional<Sku> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Sku> findByTenantIdAndBarcode(UUID tenantId, String barcode);

    List<Sku> findByTenantIdAndProductIdOrderByCodeAsc(UUID tenantId, UUID productId);

    List<Sku> findByTenantIdAndProductIdIn(UUID tenantId, Collection<UUID> productIds);

    List<Sku> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndBarcode(UUID tenantId, String barcode);

    long countByTenantIdAndProductId(UUID tenantId, UUID productId);

    /** Khoa dong SKU khi tinh lai gia von de hai phieu nhap dong thoi khong ghi de nhau. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Sku s where s.id = :id and s.tenantId = :tenantId")
    Optional<Sku> findForUpdate(@Param("id") UUID id, @Param("tenantId") UUID tenantId);
}
