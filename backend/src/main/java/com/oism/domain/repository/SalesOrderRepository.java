package com.oism.domain.repository;

import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, UUID>, JpaSpecificationExecutor<SalesOrder> {

    Optional<SalesOrder> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);

    /** Dung khi an chi nhanh: con don chua ket thuc thi khong an duoc (FR-AUTH-04). */
    boolean existsByTenantIdAndBranchIdAndStatusIn(UUID tenantId, UUID branchId, Collection<OrderStatus> statuses);
}
