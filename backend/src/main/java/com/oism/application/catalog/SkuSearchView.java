package com.oism.application.catalog;

import com.oism.domain.model.CatalogStatus;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** Dong ket qua tim SKU phang: kem ten san pham va hang kha dung. */
public record SkuSearchView(UUID id, UUID productId, String productName, String code, Map<String, String> attributes,
                            String barcode, BigDecimal retailPrice, BigDecimal wholesalePrice, BigDecimal avgCost,
                            CatalogStatus status, long available) {
}
