package com.oism.backend.service;

import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;

import java.util.List;

/**
 * Service giao diện xử lý nghiệp vụ cho sản phẩm cơ bản (Product - Tuần 1)
 */
public interface ProductService {

    // Tạo mới một sản phẩm
    ProductResponse createProduct(ProductRequest request);

    // Lấy danh sách tất cả sản phẩm của cửa hàng
    List<ProductResponse> getAllProducts();

    // Lấy thông tin chi tiết một sản phẩm theo ID
    ProductResponse getProductById(Long id);

    // Cập nhật thông tin sản phẩm theo ID
    ProductResponse updateProduct(Long id, ProductRequest request);

    // Xóa mềm sản phẩm theo ID
    void deleteProduct(Long id);
}
