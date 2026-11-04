package com.oism.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuResponse {

    private Long id;
    private String skuCode;
    private Long productId;
    private String productName;
    private String attributeValues;
    private String barcode;
    private BigDecimal costPrice;
    private BigDecimal sellingPrice;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
}
