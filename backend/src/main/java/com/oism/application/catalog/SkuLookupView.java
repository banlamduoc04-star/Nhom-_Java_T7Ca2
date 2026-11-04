package com.oism.application.catalog;

/** Ket qua tra cuu SKU theo ma vach cho POS (kem hang kha dung). */
public record SkuLookupView(SkuView sku, String productName, long available) {
}
