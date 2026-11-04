package com.oism.backend.service;

import com.oism.backend.dto.brand.BrandRequest;
import com.oism.backend.dto.brand.BrandResponse;
import com.oism.backend.entity.Brand;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.BrandRepository;
import com.oism.backend.service.impl.BrandServiceImpl;
import com.oism.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BrandServiceImplTest {

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private BrandServiceImpl brandService;

    private Tenant sampleTenant;
    private Brand sampleBrand;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(1L)
                .name("Shop Thoi Trang ABC")
                .slug("shop-thoi-trang-abc")
                .build();

        sampleBrand = Brand.builder()
                .id(10L)
                .name("Adidas")
                .description("Thuong hieu the thao")
                .tenant(sampleTenant)
                .deleted(false)
                .build();
    }

    // Test tao thuong hieu thanh cong
    @Test
    void testCreateBrandSuccess() {
        when(securityUtils.getCurrentTenant()).thenReturn(sampleTenant);
        when(brandRepository.save(any(Brand.class))).thenReturn(sampleBrand);

        BrandRequest request = new BrandRequest("Adidas", "Thuong hieu the thao");
        BrandResponse response = brandService.createBrand(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Adidas", response.getName());
        verify(brandRepository, times(1)).save(any(Brand.class));
    }

    // Test tim thuong hieu khong ton tai
    @Test
    void testGetBrandByIdNotFound() {
        Long notFoundId = 999L;
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(brandRepository.findByIdAndTenantIdAndDeletedFalse(notFoundId, 1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            brandService.getBrandById(notFoundId);
        });

        verify(brandRepository, times(1)).findByIdAndTenantIdAndDeletedFalse(notFoundId, 1L);
    }

    // Test xoa mem thuong hieu
    @Test
    void testDeleteBrandSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(brandRepository.findByIdAndTenantIdAndDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleBrand));
        when(brandRepository.save(any(Brand.class))).thenReturn(sampleBrand);

        brandService.deleteBrand(10L);

        assertTrue(sampleBrand.isDeleted());
        verify(brandRepository, times(1)).save(sampleBrand);
    }
}
