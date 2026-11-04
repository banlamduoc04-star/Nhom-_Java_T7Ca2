package com.oism.backend.service.impl;

import com.oism.backend.dto.brand.BrandRequest;
import com.oism.backend.dto.brand.BrandResponse;
import com.oism.backend.entity.Brand;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.BrandRepository;
import com.oism.backend.service.BrandService;
import com.oism.backend.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final SecurityUtils securityUtils;

    public BrandServiceImpl(BrandRepository brandRepository, SecurityUtils securityUtils) {
        this.brandRepository = brandRepository;
        this.securityUtils = securityUtils;
    }

    // Chuyen doi Entity sang DTO
    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .description(brand.getDescription())
                .build();
    }

    // Tao thuong hieu moi
    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        Tenant tenant = securityUtils.getCurrentTenant();

        Brand brand = Brand.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .tenant(tenant)
                .deleted(false)
                .build();

        return mapToResponse(brandRepository.save(brand));
    }

    // Lay danh sach thuong hieu
    @Override
    public List<BrandResponse> getAllBrands() {
        Long tenantId = securityUtils.getCurrentTenantId();
        return brandRepository.findByTenantIdAndDeletedFalse(tenantId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // Lay thuong hieu theo ID
    @Override
    public BrandResponse getBrandById(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Brand brand = brandRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thuong hieu voi ID: " + id));
        return mapToResponse(brand);
    }

    // Cap nhat thuong hieu
    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Brand brand = brandRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thuong hieu voi ID: " + id));

        brand.setName(request.getName().trim());
        brand.setDescription(request.getDescription());
        return mapToResponse(brandRepository.save(brand));
    }

    // Xoa mem thuong hieu
    @Override
    @Transactional
    public void deleteBrand(Long id) {
        Long tenantId = securityUtils.getCurrentTenantId();
        Brand brand = brandRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay thuong hieu voi ID: " + id));

        brand.setDeleted(true);
        brandRepository.save(brand);
    }
}
