package com.oism.domain.repository;

import com.oism.domain.model.Stocktake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface StocktakeRepository extends JpaRepository<Stocktake, UUID>, JpaSpecificationExecutor<Stocktake> {

    Optional<Stocktake> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
