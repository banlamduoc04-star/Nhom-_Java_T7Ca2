package com.oism.domain.repository;

import com.oism.domain.model.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BrandRepository extends JpaRepository<Brand, UUID> {

    Optional<Brand> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Brand> findByTenantIdOrderByNameAsc(UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
