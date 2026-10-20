package com.oism.backend.controller;

import com.oism.backend.dto.common.ApiResponse;
import com.oism.backend.dto.product.SkuRequest;
import com.oism.backend.dto.product.SkuResponse;
import com.oism.backend.service.SkuService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    // Them bien the SKU
    @PostMapping("/api/v1/products/{productId}/skus")
    public ResponseEntity<ApiResponse<SkuResponse>> addSku(
            @PathVariable Long productId,
            @Valid @RequestBody SkuRequest request) {
        SkuResponse response = skuService.addSkuToProduct(productId, request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Them bien the thanh cong", response), HttpStatus.CREATED);
    }

    // Lay danh sach SKU theo san pham
    @GetMapping("/api/v1/products/{productId}/skus")
    public ResponseEntity<ApiResponse<List<SkuResponse>>> getSkusByProduct(@PathVariable Long productId) {
        List<SkuResponse> response = skuService.getSkusByProductId(productId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lay danh sach bien the thanh cong", response));
    }

    // Tim SKU theo ma vach
    @GetMapping("/api/v1/skus/barcode/{barcode}")
    public ResponseEntity<ApiResponse<SkuResponse>> getSkuByBarcode(@PathVariable String barcode) {
        SkuResponse response = skuService.getSkuByBarcode(barcode);
        return ResponseEntity.ok(new ApiResponse<>(true, "Tim bien the theo ma vach thanh cong", response));
    }

    // Cap nhat SKU
    @PutMapping("/api/v1/skus/{skuId}")
    public ResponseEntity<ApiResponse<SkuResponse>> updateSku(
            @PathVariable Long skuId,
            @Valid @RequestBody SkuRequest request) {
        SkuResponse response = skuService.updateSku(skuId, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cap nhat bien the thanh cong", response));
    }

    // Xoa mem SKU
    @DeleteMapping("/api/v1/skus/{skuId}")
    public ResponseEntity<ApiResponse<Void>> deleteSku(@PathVariable Long skuId) {
        skuService.deleteSku(skuId);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xoa bien the thanh cong", null));
    }
}
