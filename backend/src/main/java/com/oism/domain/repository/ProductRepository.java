package com.oism.domain.repository;

import com.oism.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCategoryId(UUID tenantId, UUID categoryId);

    boolean existsByTenantIdAndBrandId(UUID tenantId, UUID brandId);

    List<Product> findByTenantIdAndIdIn(UUID tenantId, Collection<UUID> ids);
}
