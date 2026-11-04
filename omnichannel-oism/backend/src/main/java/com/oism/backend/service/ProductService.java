package com.oism.backend.service;

import com.oism.backend.dto.common.PageResponse;
import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts();

    PageResponse<ProductResponse> searchProducts(String keyword, Long categoryId, int page, int size, String sortBy, String sortDir);

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);
}
