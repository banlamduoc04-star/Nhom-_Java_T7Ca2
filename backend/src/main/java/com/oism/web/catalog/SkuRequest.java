package com.oism.web.catalog;

import com.oism.application.catalog.SkuInput;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

/** Du lieu tao SKU (dung cho tao san pham va them bien the). Gia > 0 (FR-PROD-04). */
public record SkuRequest(
        @NotBlank(message = "Mã SKU không được để trống")
        @Size(max = 64, message = "Mã SKU tối đa 64 ký tự")
        String code,
        Map<String, String> attributes,
        @Size(max = 64, message = "Mã vạch tối đa 64 ký tự")
        String barcode,
        @NotNull(message = "Giá bán lẻ không được để trống")
        @DecimalMin(value = "0.01", message = "Giá bán lẻ phải lớn hơn 0")
        BigDecimal retailPrice,
        @DecimalMin(value = "0.01", message = "Giá bán sỉ phải lớn hơn 0")
        BigDecimal wholesalePrice,
        @Min(value = 0, message = "Ngưỡng đặt hàng lại không được âm")
        Integer reorderPoint) {

    SkuInput toInput() {
        return new SkuInput(code, attributes, barcode, retailPrice, wholesalePrice, reorderPoint);
    }
}
