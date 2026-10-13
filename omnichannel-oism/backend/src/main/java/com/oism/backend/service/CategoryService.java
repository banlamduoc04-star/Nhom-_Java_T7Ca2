package com.oism.backend.service;

import com.oism.backend.dto.category.CategoryRequest;
import com.oism.backend.dto.category.CategoryResponse;

import java.util.List;

/**
 * Service giao diện xử lý nghiệp vụ cho danh mục sản phẩm (Category).
 */
public interface CategoryService {

    // Tạo mới một danh mục
    CategoryResponse createCategory(CategoryRequest request);

    // Lấy thông tin danh mục theo ID
    CategoryResponse getCategoryById(Long id);

    // Lấy danh sách tất cả danh mục của cửa hàng
    List<CategoryResponse> getAllCategories();

    // Cập nhật thông tin danh mục theo ID
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    // Xóa mềm danh mục theo ID
    void deleteCategory(Long id);
}
