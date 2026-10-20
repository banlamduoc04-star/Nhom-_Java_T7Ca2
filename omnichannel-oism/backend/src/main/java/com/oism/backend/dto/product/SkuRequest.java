package com.oism.backend.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class SkuRequest {

    @NotBlank(message = "Mã SKU không được để trống")
    private String skuCode;

    private String attributeValues;

    private String barcode;

    @NotNull(message = "Giá nhập không được để trống")
    @DecimalMin(value = "0", message = "Giá nhập phải >= 0")
    private BigDecimal costPrice;

    @NotNull(message = "Giá bán không được để trống")
    @Positive(message = "Giá bán phải > 0")
    private BigDecimal sellingPrice;

    private BigDecimal retailPrice;

    private BigDecimal wholesalePrice;

    public SkuRequest(String skuCode, String attributeValues, String barcode, BigDecimal costPrice, BigDecimal sellingPrice) {
        this.skuCode = skuCode;
        this.attributeValues = attributeValues;
        this.barcode = barcode;
        this.costPrice = costPrice;
        this.sellingPrice = sellingPrice;
        this.retailPrice = sellingPrice;
    }
}
