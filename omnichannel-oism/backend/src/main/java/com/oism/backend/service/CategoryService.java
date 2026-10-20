package com.oism.backend.service;

import com.oism.backend.dto.category.CategoryRequest;
import com.oism.backend.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse getCategoryById(Long id);

    List<CategoryResponse> getAllCategories();

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);
}
