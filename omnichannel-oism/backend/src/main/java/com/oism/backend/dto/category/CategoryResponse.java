package com.oism.backend.dto.category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về thông tin chi tiết của danh mục sản phẩm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    // Mã định danh danh mục
    private Long id;

    // Tên danh mục
    private String name;

    // Mô tả danh mục
    private String description;
}
