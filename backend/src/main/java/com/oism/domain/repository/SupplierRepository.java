package com.oism.domain.repository;

import com.oism.domain.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {

    Optional<Supplier> findByIdAndTenantId(UUID id, UUID tenantId);

    List<Supplier> findByTenantIdOrderByNameAsc(UUID tenantId);
}
