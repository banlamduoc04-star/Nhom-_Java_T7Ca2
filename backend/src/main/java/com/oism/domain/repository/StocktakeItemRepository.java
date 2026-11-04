package com.oism.domain.repository;

import com.oism.domain.model.StocktakeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, UUID> {

    List<StocktakeItem> findByTenantIdAndStocktakeId(UUID tenantId, UUID stocktakeId);

    void deleteByTenantIdAndStocktakeId(UUID tenantId, UUID stocktakeId);
}
