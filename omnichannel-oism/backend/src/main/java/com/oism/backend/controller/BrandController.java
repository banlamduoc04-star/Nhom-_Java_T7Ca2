package com.oism.backend.controller;

import com.oism.backend.dto.brand.BrandRequest;
import com.oism.backend.dto.brand.BrandResponse;
import com.oism.backend.dto.common.ApiResponse;
import com.oism.backend.service.BrandService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    // Tao thuong hieu moi
    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return new ResponseEntity<>(new ApiResponse<>(true, "Tao thuong hieu thanh cong", response), HttpStatus.CREATED);
    }

    // Lay tat ca thuong hieu
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        List<BrandResponse> response = brandService.getAllBrands();
        return ResponseEntity.ok(new ApiResponse<>(true, "Lay danh sach thuong hieu thanh cong", response));
    }

    // Lay thuong hieu theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable Long id) {
        BrandResponse response = brandService.getBrandById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Lay thuong hieu thanh cong", response));
    }

    // Cap nhat thuong hieu
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Cap nhat thuong hieu thanh cong", response));
    }

    // Xoa mem thuong hieu
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Xoa thuong hieu thanh cong", null));
    }
}
