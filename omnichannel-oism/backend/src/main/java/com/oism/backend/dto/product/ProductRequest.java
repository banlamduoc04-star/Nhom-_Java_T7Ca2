package com.oism.backend.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận dữ liệu yêu cầu tạo mới hoặc cập nhật thông tin sản phẩm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductRequest {

    // Tên sản phẩm bắt buộc nhập
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    // Mô tả chi tiết sản phẩm
    private String description;

    // Đường dẫn hình ảnh minh họa sản phẩm
    private String imageUrl;

    // ID danh mục mà sản phẩm thuộc về
    @NotNull(message = "Danh mục không được để trống")
    private Long categoryId;
}
