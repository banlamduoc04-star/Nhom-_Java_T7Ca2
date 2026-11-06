package com.oism.domain.repository;

import com.oism.domain.model.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAlertRepository extends JpaRepository<StockAlert, UUID> {

    Optional<StockAlert> findByTenantIdAndBranchIdAndSkuIdAndStatus(
            UUID tenantId, UUID branchId, UUID skuId, StockAlert.Status status);

    List<StockAlert> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<StockAlert> findByTenantIdAndStatusOrderByCreatedAtDesc(UUID tenantId, StockAlert.Status status);

    List<StockAlert> findByTenantIdAndBranchIdAndStatusOrderByCreatedAtDesc(
            UUID tenantId, UUID branchId, StockAlert.Status status);

    long countByTenantIdAndStatus(UUID tenantId, StockAlert.Status status);
}

