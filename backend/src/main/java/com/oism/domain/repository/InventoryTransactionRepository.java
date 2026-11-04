package com.oism.domain.repository;

import com.oism.domain.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

/** Chi doc va them moi; khong khai bao ham sua/xoa nao cho so cai. */
public interface InventoryTransactionRepository
        extends JpaRepository<InventoryTransaction, UUID>, JpaSpecificationExecutor<InventoryTransaction> {

    boolean existsByTenantIdAndSkuId(UUID tenantId, UUID skuId);
}
