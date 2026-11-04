package com.oism.application.catalog;

import java.math.BigDecimal;
import java.util.Map;

/** Du lieu tao mot SKU (dung khi tao san pham nhieu bien the mot lan va khi them SKU le). */
public record SkuInput(String code, Map<String, String> attributes, String barcode,
                       BigDecimal retailPrice, BigDecimal wholesalePrice, Integer reorderPoint) {
}
