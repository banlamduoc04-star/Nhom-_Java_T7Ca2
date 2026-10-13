package com.oism.backend.dto.category;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO nhận dữ liệu yêu cầu tạo mới hoặc cập nhật danh mục.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    // Tên danh mục bắt buộc nhập
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    // Mô tả chi tiết về danh mục
    private String description;
}
