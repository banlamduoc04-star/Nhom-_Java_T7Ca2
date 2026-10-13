package com.oism.backend.controller;

import com.oism.backend.dto.category.CategoryRequest;
import com.oism.backend.dto.category.CategoryResponse;
import com.oism.backend.dto.common.ApiResponse;
import com.oism.backend.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller xử lý các yêu cầu liên quan đến danh mục sản phẩm.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // Tiêm phụ thuộc CategoryService qua constructor
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    // Tạo danh mục mới
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Tạo danh mục thành công", response), HttpStatus.CREATED);
    }

    // Lấy danh sách tất cả danh mục
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> response = categoryService.getAllCategories();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách danh mục thành công", response));
    }

    // Lấy chi tiết danh mục theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse response = categoryService.getCategoryById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh mục thành công", response));
    }

    // Cập nhật thông tin danh mục theo ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật danh mục thành công", response));
    }

    // Xóa mềm danh mục theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xóa danh mục thành công", null));
    }
}
