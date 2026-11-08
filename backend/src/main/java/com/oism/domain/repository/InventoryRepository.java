package com.oism.domain.repository;

import com.oism.infrastructure.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {

    Optional<InventoryEntity> findByTenantIdAndSkuCode(String tenantId, String skuCode);
}