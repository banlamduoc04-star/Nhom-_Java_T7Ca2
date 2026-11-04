package com.oism.domain.repository;

import com.oism.domain.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID> {

    /** Luon truy van kem tenant_id: findById khong bi bo loc tenant chan (NFR-TENANT-01). */
    Optional<Branch> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Branch> findByTenantIdOrderByNameAsc(UUID tenantId);

    boolean existsByTenantIdAndNameIgnoreCase(UUID tenantId, String name);
}
