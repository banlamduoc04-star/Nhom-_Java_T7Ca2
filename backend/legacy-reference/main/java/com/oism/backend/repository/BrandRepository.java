package com.oism.backend.repository;

import com.oism.backend.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByTenantIdAndDeletedFalse(Long tenantId);

    Optional<Brand> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    boolean existsByNameAndTenantIdAndDeletedFalse(String name, Long tenantId);
}
