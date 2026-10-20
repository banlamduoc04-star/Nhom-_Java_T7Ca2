package com.oism.backend.repository;

import com.oism.backend.entity.Sku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkuRepository extends JpaRepository<Sku, Long> {

    List<Sku> findByProductIdAndDeletedFalse(Long productId);

    Optional<Sku> findByIdAndDeletedFalse(Long id);

    boolean existsBySkuCodeAndTenantIdAndDeletedFalse(String skuCode, Long tenantId);

    boolean existsByBarcodeAndTenantIdAndDeletedFalse(String barcode, Long tenantId);

    Optional<Sku> findByBarcodeAndTenantIdAndDeletedFalse(String barcode, Long tenantId);
}
