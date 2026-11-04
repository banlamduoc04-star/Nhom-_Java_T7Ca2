package com.oism.application.catalog;

import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Sku;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record SkuView(UUID id, UUID productId, String code, Map<String, String> attributes, String barcode,
                      BigDecimal retailPrice, BigDecimal wholesalePrice, BigDecimal avgCost,
                      int reorderPoint, CatalogStatus status) {

    static SkuView from(Sku s) {
        return new SkuView(s.getId(), s.getProductId(), s.getCode(), s.getAttributes(), s.getBarcode(),
                s.getRetailPrice(), s.getWholesalePrice(), s.getAvgCost(), s.getReorderPoint(), s.getStatus());
    }
}
