package com.oism.backend.service;

import com.oism.backend.dto.product.SkuRequest;
import com.oism.backend.dto.product.SkuResponse;
import com.oism.backend.entity.Product;
import com.oism.backend.entity.Sku;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.DuplicateResourceException;
import com.oism.backend.repository.ProductRepository;
import com.oism.backend.repository.SkuRepository;
import com.oism.backend.service.impl.SkuServiceImpl;
import com.oism.backend.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SkuServiceImplTest {

    @Mock
    private SkuRepository skuRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private SkuServiceImpl skuService;

    private Tenant sampleTenant;
    private Product sampleProduct;
    private Sku sampleSku;

    @BeforeEach
    void setUp() {
        sampleTenant = Tenant.builder()
                .id(1L)
                .name("Shop Thoi Trang ABC")
                .slug("shop-thoi-trang-abc")
                .build();

        sampleProduct = Product.builder()
                .id(100L)
                .name("Ao Thun Co Tron")
                .tenant(sampleTenant)
                .deleted(false)
                .build();

        sampleSku = Sku.builder()
                .id(1001L)
                .skuCode("AT-DEN-L")
                .product(sampleProduct)
                .attributeValues("Mau: Den, Size: L")
                .barcode("89350000001")
                .costPrice(new BigDecimal("100000"))
                .sellingPrice(new BigDecimal("199000"))
                .retailPrice(new BigDecimal("199000"))
                .wholesalePrice(new BigDecimal("160000"))
                .tenant(sampleTenant)
                .deleted(false)
                .build();
    }

    // Test them bien the SKU thanh cong
    @Test
    void testAddSkuSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(productRepository.findByIdAndTenantIdAndDeletedFalse(100L, 1L))
                .thenReturn(Optional.of(sampleProduct));
        when(skuRepository.existsBySkuCodeAndTenantIdAndDeletedFalse("AT-DEN-L", 1L))
                .thenReturn(false);
        when(skuRepository.existsByBarcodeAndTenantIdAndDeletedFalse("89350000001", 1L))
                .thenReturn(false);
        when(skuRepository.save(any(Sku.class))).thenReturn(sampleSku);

        SkuRequest request = new SkuRequest("AT-DEN-L", "Mau: Den, Size: L", "89350000001",
                new BigDecimal("100000"), new BigDecimal("199000"));

        SkuResponse response = skuService.addSkuToProduct(100L, request);

        assertNotNull(response);
        assertEquals("AT-DEN-L", response.getSkuCode());
        assertEquals("89350000001", response.getBarcode());
        verify(skuRepository, times(1)).save(any(Sku.class));
    }

    // Test them SKU that bai do trung ma skuCode
    @Test
    void testAddSkuDuplicateSkuCode() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(productRepository.findByIdAndTenantIdAndDeletedFalse(100L, 1L))
                .thenReturn(Optional.of(sampleProduct));
        when(skuRepository.existsBySkuCodeAndTenantIdAndDeletedFalse("AT-DEN-L", 1L))
                .thenReturn(true);

        SkuRequest request = new SkuRequest("AT-DEN-L", "Mau: Den, Size: L", "89350000001",
                new BigDecimal("100000"), new BigDecimal("199000"));

        assertThrows(DuplicateResourceException.class, () -> {
            skuService.addSkuToProduct(100L, request);
        });

        verify(skuRepository, never()).save(any(Sku.class));
    }

    // Test tim SKU theo ma vach barcode
    @Test
    void testGetSkuByBarcodeSuccess() {
        when(securityUtils.getCurrentTenantId()).thenReturn(1L);
        when(skuRepository.findByBarcodeAndTenantIdAndDeletedFalse("89350000001", 1L))
                .thenReturn(Optional.of(sampleSku));

        SkuResponse response = skuService.getSkuByBarcode("89350000001");

        assertNotNull(response);
        assertEquals("89350000001", response.getBarcode());
        assertEquals("AT-DEN-L", response.getSkuCode());
    }
}
