package com.oism.domain.repository;

import com.oism.domain.model.PurchaseReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PurchaseReceiptItemRepository extends JpaRepository<PurchaseReceiptItem, UUID> {

    List<PurchaseReceiptItem> findByTenantIdAndReceiptId(UUID tenantId, UUID receiptId);

    List<PurchaseReceiptItem> findByTenantIdAndReceiptIdIn(UUID tenantId, Collection<UUID> receiptIds);

    void deleteByTenantIdAndReceiptId(UUID tenantId, UUID receiptId);
}
