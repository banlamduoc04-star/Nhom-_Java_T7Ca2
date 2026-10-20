package com.oism.backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    private String imageUrl;

    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;

    private Long brandId;

    public ProductRequest(String name, String description, String imageUrl, Long categoryId) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
    }
}
