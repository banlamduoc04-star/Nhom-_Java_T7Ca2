package com.oism.backend.service;

import com.oism.backend.dto.brand.BrandRequest;
import com.oism.backend.dto.brand.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse createBrand(BrandRequest request);

    List<BrandResponse> getAllBrands();

    BrandResponse getBrandById(Long id);

    BrandResponse updateBrand(Long id, BrandRequest request);

    void deleteBrand(Long id);
}
