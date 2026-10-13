package com.oism.backend.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về thông tin cơ bản của sản phẩm trong danh sách hoặc kết quả tìm kiếm.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    // Mã định danh sản phẩm
    private Long id;

    // Tên sản phẩm
    private String name;

    // Mô tả sản phẩm
    private String description;

    // Đường dẫn hình ảnh minh họa sản phẩm
    private String imageUrl;

    // Mã định danh danh mục của sản phẩm
    private Long categoryId;

    // Tên danh mục hiển thị
    private String categoryName;

    // Trạng thái kích hoạt của sản phẩm
    private boolean isActive;
}
