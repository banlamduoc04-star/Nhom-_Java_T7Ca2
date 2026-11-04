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

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    public CategoryServiceImpl(CategoryRepository categoryRepository, SecurityUtils securityUtils) {
        this.categoryRepository = categoryRepository;
        this.securityUtils = securityUtils;
    }

    // Chuyen doi Entity sang DTO
    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .build();
    }

    // Tao danh muc moi
    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Tenant tenant = securityUtils.getCurrentTenant();

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getParentId(), tenant.getId())
                    .orElse(null);
        }

        Category category = Category.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .parent(parent)
                .tenant(tenant)
                .deleted(false)
                .build();

        return mapToResponse(categoryRepository.save(category));
    }

    // Lay danh muc theo ID
    @Override
    public CategoryResponse getCategoryById(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + id));
        return mapToResponse(category);
    }

    // Lay danh sach tat ca danh muc
    @Override
    public List<CategoryResponse> getAllCategories() {
        Long tenantId = securityUtils.getCurrentTenantId();
        return categoryRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Cap nhat danh muc
    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + id));

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getParentId(), tenantId)
                    .orElse(null);
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        return mapToResponse(categoryRepository.save(category));
    }

    // Xoa mem danh muc
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + id));

        category.setDeleted(true);
        categoryRepository.save(category);
    }
}
