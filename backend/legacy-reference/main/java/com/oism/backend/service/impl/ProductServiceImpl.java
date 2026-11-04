package com.oism.backend.service.impl;

import com.oism.backend.dto.common.PageResponse;
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
import com.oism.backend.service.ProductService;
import com.oism.backend.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SecurityUtils securityUtils;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              BrandRepository brandRepository,
                              SecurityUtils securityUtils) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.securityUtils = securityUtils;
    }

    // Chuyen doi Entity sang DTO
    private ProductResponse mapToProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .isActive(product.isActive())
                .build();
    }

    // Tao san pham moi
    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Tenant tenant = securityUtils.getCurrentTenant();

        Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getCategoryId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + request.getCategoryId()));

        Brand brand = null;
        if (request.getBrandId() != null) {
            brand = brandRepository.findByIdAndTenantIdAndDeletedFalse(request.getBrandId(), tenantId).orElse(null);
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(category)
                .brand(brand)
                .tenant(tenant)
                .isActive(true)
                .deleted(false)
                .build();

        return mapToProductResponse(productRepository.save(product));
    }

    // Lay danh sach san pham
    @Override
    public List<ProductResponse> getAllProducts() {
        Long tenantId = securityUtils.getCurrentTenantId();
        return productRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    // Tim kiem va phan trang san pham
    @Override
    public PageResponse<ProductResponse> searchProducts(String keyword, Long categoryId, int page, int size, String sortBy, String sortDir) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Sort sort = "desc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchKeyword = (keyword == null) ? "" : keyword.trim();
        Page<Product> productPage;
        if (categoryId != null && categoryId > 0) {
            productPage = productRepository.searchByKeywordAndCategory(tenantId, searchKeyword, categoryId, pageable);
        } else {
            productPage = productRepository.searchByKeyword(tenantId, searchKeyword, pageable);
        }

        List<ProductResponse> content = productPage.getContent()
                .stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages(),
                productPage.isLast()
        );
    }

    // Lay chi tiet san pham theo ID
    @Override
    public ProductResponse getProductById(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi ID: " + id));
        return mapToProductResponse(product);
    }

    // Cap nhat san pham
    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi ID: " + id));

        if (!product.getCategory().getId().equals(request.getCategoryId())) {
            Category category = categoryRepository.findByIdAndTenantIdAndDeletedFalse(request.getCategoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay danh muc voi ID: " + request.getCategoryId()));
            product.setCategory(category);
        }

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findByIdAndTenantIdAndDeletedFalse(request.getBrandId(), tenantId).orElse(null);
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());

        return mapToProductResponse(productRepository.save(product));
    }

    // Xoa mem san pham
    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi ID: " + id));

        product.setDeleted(true);
        productRepository.save(product);
    }
}
