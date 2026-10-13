package com.oism.backend.service.impl;

import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;
import com.oism.backend.entity.Category;
import com.oism.backend.entity.Product;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.CategoryRepository;
import com.oism.backend.repository.ProductRepository;
import com.oism.backend.service.ProductService;
import com.oism.backend.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Cài đặt nghiệp vụ quản lý sản phẩm cơ bản (Tuần 1)
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;

    // Tiêm các dependency qua Constructor Injection
    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.securityUtils = securityUtils;
    }

    // Chuyển đổi từ Product Entity sang ProductResponse DTO
    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .isActive(product.isActive())
                .build();
    }

    // Tạo mới một sản phẩm
    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Tenant tenant = securityUtils.getCurrentTenant();

        // Kiểm tra danh mục có tồn tại và thuộc cùng cửa hàng không
        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getCategoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));

        // Tạo mới entity Product
        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(category)
                .tenant(tenant)
                .isActive(true)
                .deleted(false)
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToProductResponse(savedProduct);
    }

    // Lấy danh sách tất cả sản phẩm của cửa hàng
    @Override
    public List<ProductResponse> getAllProducts() {
        Long tenantId = securityUtils.getCurrentTenantId();

        return productRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    // Lấy chi tiết một sản phẩm theo ID
    @Override
    public ProductResponse getProductById(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        return mapToProductResponse(product);
    }

    // Cập nhật thông tin sản phẩm
    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        // Nếu thay đổi danh mục thì kiểm tra lại
        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getCategoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục với ID: " + request.getCategoryId()));
            product.setCategory(category);
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        return mapToProductResponse(updatedProduct);
    }

    // Xóa mềm sản phẩm (deleted = true)
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));

        product.setDeleted(true);
        productRepository.save(product);
    }
}
