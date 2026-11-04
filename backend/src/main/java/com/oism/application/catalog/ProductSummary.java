package com.oism.application.catalog;

import com.oism.domain.model.CatalogStatus;

import java.math.BigDecimal;
import java.util.UUID;

/** Dong trong danh sach san pham: kem so SKU va khoang gia ban le. */
public record ProductSummary(UUID id, String name, UUID categoryId, UUID brandId, boolean hasVariants,
                             CatalogStatus status, int skuCount, BigDecimal minRetailPrice, BigDecimal maxRetailPrice) {
}
