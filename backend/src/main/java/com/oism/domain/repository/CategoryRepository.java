package com.oism.domain.repository;

import com.oism.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    /** Luon truy van kem tenant_id: findById khong bi bo loc tenant chan (NFR-TENANT-01). */
    Optional<Category> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Category> findByTenantIdOrderByNameAsc(UUID tenantId);

    boolean existsByTenantIdAndParentIdAndNameIgnoreCase(UUID tenantId, UUID parentId, String name);

    boolean existsByTenantIdAndParentId(UUID tenantId, UUID parentId);
}
