package com.oism.backend.service;

import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;
import com.oism.backend.entity.Brand;
import com.oism.backend.entity.Category;
import com.oism.backend.entity.Product;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.BrandRepository;
import com.oism.backend.repository.CategoryRepository;
import com.oism.backend.repository.ProductRepository;
import com.oism.backend.service.impl.ProductServiceImpl;
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
public class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private ProductServiceImpl productService;

    private Tenant sampleTenant;
    private Category sampleCategory;
    private Brand sampleBrand;
    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(1L)
                .name("Cua hang Demo")
                .slug("demo-store")
                .build();

        sampleCategory = Category.builder()
                .id(10L)
                .name("Thoi trang Nam")
                .tenant(sampleTenant)
                .build();

        sampleBrand = Brand.builder()
                .id(5L)
                .name("Nike")
                .tenant(sampleTenant)
                .build();

        sampleProduct = Product.builder()
                .id(100L)
                .name("Ao Polo Nam")
                .description("Chat lieu cotton thoang mat")
                .imageUrl("https://example.com/polo.jpg")
                .category(sampleCategory)
                .brand(sampleBrand)
                .tenant(sampleTenant)
                .isActive(true)
                .deleted(false)
                .build();
    }

    // Test tao san pham moi thanh cong
    @Test
    void testCreateProductSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(securityUtils.getCurrentTenant()).thenReturn(sampleTenant);
        when(categoryRepository.findByIdAndTenantIdAndDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleCategory));
        when(brandRepository.findByIdAndTenantIdAndDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(sampleBrand));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductRequest request = new ProductRequest("Ao Polo Nam", "Chat lieu cotton", "https://example.com/polo.jpg", 10L, 5L);
        ProductResponse response = productService.createProduct(request);

        assertNotNull(response);
        assertEquals("Ao Polo Nam", response.getName());
        assertEquals(10L, response.getCategoryId());
        assertEquals(5L, response.getBrandId());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    // Test tao san pham that bai do khong tim thay danh muc
    @Test
    void testCreateProductCategoryNotFound() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(categoryRepository.findByIdAndTenantIdAndDeletedFalse(99L, 1L))
                .thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("San pham loi", "Mo ta", "url", 99L, null);

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProduct(request);
        });

        verify(productRepository, never()).save(any(Product.class));
    }

    // Test lay chi tiet san pham theo ID
    @Test
    void testGetProductByIdSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(productRepository.findByIdAndTenantIdAndDeletedFalse(100L, 1L))
                .thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Ao Polo Nam", response.getName());
    }

    // Test xoa mem san pham
    @Test
    void testDeleteProductSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(productRepository.findByIdAndTenantIdAndDeletedFalse(100L, 1L))
                .thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.deleteProduct(100L);

        assertTrue(sampleProduct.isDeleted());
        verify(productRepository, times(1)).save(sampleProduct);
    }
}
