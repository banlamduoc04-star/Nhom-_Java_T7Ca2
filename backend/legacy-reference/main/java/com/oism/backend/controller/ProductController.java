package com.oism.backend.controller;

import com.oism.backend.dto.common.ApiResponse;
import com.oism.backend.dto.common.PageResponse;
import com.oism.backend.dto.product.ProductRequest;
import com.oism.backend.dto.product.ProductResponse;
import com.oism.backend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Tao san pham moi
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Tao san pham thanh cong", response), HttpStatus.CREATED);
    }

    // Lay tat ca san pham
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        List<ProductResponse> response = productService.getAllProducts();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lay danh sach san pham thanh cong", response));
    }

    // Tim kiem va phan trang san pham
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<ProductResponse> response = productService.searchProducts(keyword, categoryId, page, size, sortBy, sortDir);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tim kiem san pham thanh cong", response));
    }

    // Lay chi tiet san pham
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lay chi tiet san pham thanh cong", response));
    }

    // Cap nhat san pham
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cap nhat san pham thanh cong", response));
    }

    // Xoa mem san pham
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xoa san pham thanh cong", null));
    }
}
