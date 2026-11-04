package com.oism.backend.service;

import com.oism.backend.dto.category.CategoryRequest;
import com.oism.backend.dto.category.CategoryResponse;
import com.oism.backend.entity.Category;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.CategoryRepository;
import com.oism.backend.service.impl.CategoryServiceImpl;
import com.oism.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Tenant sampleTenant;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(1L)
                .name("Shop Thoi Trang ABC")
                .slug("shop-thoi-trang-abc")
                .build();

        sampleCategory = Category.builder()
                .id(10L)
                .name("Ao So Mi")
                .description("Cac loai ao so mi")
                .tenant(sampleTenant)
                .deleted(false)
                .build();
    }

    // Test tao danh muc thanh cong
    @Test
    void testCreateCategorySuccess() {
        when(securityUtils.getCurrentTenant()).thenReturn(sampleTenant);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        CategoryRequest request = new CategoryRequest("Ao So Mi", "Cac loai ao so mi");
        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Ao So Mi", response.getName());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    // Test tim danh muc khong ton tai
    @Test
    void testGetCategoryByIdNotFound() {
        Long notFoundId = 999L;
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(categoryRepository.findByIdAndTenantIdAndDeletedFalse(notFoundId, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.getCategoryById(notFoundId);
        });

        verify(categoryRepository, times(1)).findByIdAndTenantIdAndDeletedFalse(notFoundId, 1L);
    }

    // Test xoa mem danh muc
    @Test
    void testDeleteCategorySuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(categoryRepository.findByIdAndTenantIdAndDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);

        categoryService.deleteCategory(10L);

        assertTrue(sampleCategory.isDeleted());
        verify(categoryRepository, times(1)).save(sampleCategory);
    }
}
