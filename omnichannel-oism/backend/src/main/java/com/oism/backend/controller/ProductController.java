package com.oism.backend.controller;

import com.oism.backend.dto.common.ApiResponse;
import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;
import com.oism.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các yêu cầu liên quan đến sản phẩm cơ bản (Tuần 1)
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    // Tiêm phụ thuộc ProductService qua constructor
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Tạo sản phẩm mới
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Tạo sản phẩm thành công", response), HttpStatus.CREATED);
    }

    // Lấy danh sách tất cả sản phẩm
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> response = productService.getAllProducts();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy danh sách sản phẩm thành công", response));
    }

    // Lấy thông tin chi tiết một sản phẩm theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lấy chi tiết sản phẩm thành công", response));
    }

    // Cập nhật thông tin sản phẩm theo ID
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cập nhật sản phẩm thành công", response));
    }

    // Xóa mềm sản phẩm theo ID
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xóa sản phẩm thành công", null));
    }
}
