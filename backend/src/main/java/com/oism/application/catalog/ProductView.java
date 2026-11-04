package com.oism.application.catalog;

import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Product;

import java.util.List;
import java.util.UUID;

public record ProductView(UUID id, String name, String description, UUID categoryId, UUID brandId,
                          boolean hasVariants, CatalogStatus status, List<SkuView> skus) {

    static ProductView from(Product p, List<SkuView> skus) {
        return new ProductView(p.getId(), p.getName(), p.getDescription(), p.getCategoryId(), p.getBrandId(),
                p.isHasVariants(), p.getStatus(), skus);
    }
}
