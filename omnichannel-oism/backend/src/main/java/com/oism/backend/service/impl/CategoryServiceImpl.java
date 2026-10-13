package com.oism.backend.service.impl;

import com.oism.backend.dto.category.CategoryRequest;
import com.oism.backend.dto.category.CategoryResponse;
import com.oism.backend.entity.Category;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.CategoryRepository;
import com.oism.backend.service.CategoryService;
import com.oism.backend.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Lớp cài đặt xử lý nghiệp vụ cho danh mục sản phẩm (Category)
 */
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    // Tiêm phụ thuộc qua Constructor Injection
    public CategoryServiceImpl(CategoryRepository categoryRepository, SecurityUtils securityUtils) {
        this.categoryRepository = categoryRepository;
        this.securityUtils = securityUtils;
    }

    // Chuyển đổi từ Entity sang DTO Response
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    // Tạo mới danh mục
    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Tenant tenant = securityUtils.getCurrentTenant();

        // Xây dựng đối tượng danh mục mới
        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .tenant(tenant)
                .deleted(false)
                .build();

        Category savedCategory = categoryRepository.save(category);
        return mapToResponse(savedCategory);
    }

    // Lấy thông tin danh mục theo ID
    @Override
    public CategoryResponse getCategoryById(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();

        // Tìm danh mục theo ID và tenantId, loại bỏ bản ghi đã xóa mềm
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        return mapToResponse(category);
    }

    // Lấy danh sách tất cả danh mục chưa bị xóa
    @Override
    public List<CategoryResponse> getAllCategories() {
        Long tenantId = securityUtils.getCurrentTenantId();

        return categoryRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Cập nhật thông tin danh mục
    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();

        // Kiểm tra danh mục có tồn tại không
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // Cập nhật thông tin mới
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        Category updatedCategory = categoryRepository.save(category);
        return mapToResponse(updatedCategory);
    }

    // Xóa mềm danh mục (cập nhật deleted = true, không xóa vật lý khỏi CSDL)
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + id));

        // Đánh dấu xóa mềm
        category.setDeleted(true);
        categoryRepository.save(category);
    }
}
