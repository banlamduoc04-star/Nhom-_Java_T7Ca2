package com.oism.domain.repository;

import com.oism.domain.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    List<Payment> findByTenantId(UUID tenantId);
}
