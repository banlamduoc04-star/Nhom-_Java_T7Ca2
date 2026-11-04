package com.oism.domain.repository;

import com.oism.domain.model.PurchaseReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface PurchaseReceiptRepository
        extends JpaRepository<PurchaseReceipt, UUID>, JpaSpecificationExecutor<PurchaseReceipt> {

    Optional<PurchaseReceipt> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
