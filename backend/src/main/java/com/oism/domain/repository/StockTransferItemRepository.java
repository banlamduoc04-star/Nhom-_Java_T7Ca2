package com.oism.domain.repository;

import com.oism.domain.model.StockTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockTransferItemRepository extends JpaRepository<StockTransferItem, UUID> {

    List<StockTransferItem> findByTenantIdAndTransferId(UUID tenantId, UUID transferId);

    void deleteByTenantIdAndTransferId(UUID tenantId, UUID transferId);
}
