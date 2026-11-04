package com.oism.domain.repository;

import com.oism.domain.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, UUID> {

    List<OrderStatusHistory> findByTenantIdAndOrderIdOrderByCreatedAtAsc(UUID tenantId, UUID orderId);
}
