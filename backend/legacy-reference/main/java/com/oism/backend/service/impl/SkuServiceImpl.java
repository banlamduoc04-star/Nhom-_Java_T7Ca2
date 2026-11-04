package com.oism.backend.service.impl;

import com.oism.backend.dto.product.SkuRequest;
import com.oism.backend.dto.product.SkuResponse;
import com.oism.backend.entity.Product;
import com.oism.backend.entity.Sku;
import com.oism.backend.entity.Tenant;
import com.oism.backend.exception.DuplicateResourceException;
import com.oism.backend.exception.ResourceNotFoundException;
import com.oism.backend.repository.ProductRepository;
import com.oism.backend.repository.SkuRepository;
import com.oism.backend.service.SkuService;
import com.oism.backend.util.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SkuServiceImpl implements SkuService {

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final SecurityUtils securityUtils;

    public SkuServiceImpl(SkuRepository skuRepository,
                          ProductRepository productRepository,
                          SecurityUtils securityUtils) {
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.securityUtils = securityUtils;
    }

    // Them bien the SKU cho san pham
    @Override
    @Transactional
    public SkuResponse addSkuToProduct(Long productId, SkuRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Product product = productRepository.findByIdAndTenantIdAndDeletedFalse(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi ID: " + productId));

        if (skuRepository.existsBySkuCodeAndTenantIdAndDeletedFalse(request.getSkuCode(), tenantId)) {
            throw new DuplicateResourceException("Ma SKU '" + request.getSkuCode() + "' da ton tai");
        }

        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            if (skuRepository.existsByBarcodeAndTenantIdAndDeletedFalse(request.getBarcode(), tenantId)) {
                throw new DuplicateResourceException("Ma vach '" + request.getBarcode() + "' da ton tai");
            }
        }

        BigDecimal retailPrice = request.getRetailPrice() != null ? request.getRetailPrice() : request.getSellingPrice();
        BigDecimal sellingPrice = request.getSellingPrice() != null ? request.getSellingPrice() : retailPrice;

        Tenant tenant = product.getTenant();
        Sku sku = Sku.builder()
                .skuCode(request.getSkuCode().trim())
                .product(product)
                .attributeValues(request.getAttributeValues())
                .barcode(request.getBarcode() != null ? request.getBarcode().trim() : null)
                .costPrice(request.getCostPrice())
                .sellingPrice(sellingPrice)
                .retailPrice(retailPrice)
                .wholesalePrice(request.getWholesalePrice())
                .tenant(tenant)
                .deleted(false)
                .build();

        return mapToResponse(skuRepository.save(sku));
    }

    // Lay danh sach bien the SKU cua san pham
    @Override
    public List<SkuResponse> getSkusByProductId(Long productId) {
        Long tenantId = securityUtils.getCurrentTenantId();

        productRepository.findByIdAndTenantIdAndDeletedFalse(productId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay san pham voi ID: " + productId));

        return skuRepository.findByProductIdAndDeletedFalse(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Tim bien the theo ma vach barcode
    @Override
    public SkuResponse getSkuByBarcode(String barcode) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Sku sku = skuRepository.findByBarcodeAndTenantIdAndDeletedFalse(barcode, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bien the voi ma vach: " + barcode));

        return mapToResponse(sku);
    }

    // Cap nhat bien the SKU
    @Override
    @Transactional
    public SkuResponse updateSku(Long skuId, SkuRequest request) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Sku sku = skuRepository.findByIdAndDeletedFalse(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bien the voi ID: " + skuId));

        if (sku.getTenant() == null || !sku.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Khong tim thay bien the voi ID: " + skuId);
        }

        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            if (!request.getBarcode().equalsIgnoreCase(sku.getBarcode())) {
                if (skuRepository.existsByBarcodeAndTenantIdAndDeletedFalse(request.getBarcode(), tenantId)) {
                    throw new DuplicateResourceException("Ma vach '" + request.getBarcode() + "' da ton tai");
                }
            }
        }

        BigDecimal retailPrice = request.getRetailPrice() != null ? request.getRetailPrice() : request.getSellingPrice();
        BigDecimal sellingPrice = request.getSellingPrice() != null ? request.getSellingPrice() : retailPrice;

        sku.setCostPrice(request.getCostPrice());
        sku.setSellingPrice(sellingPrice);
        sku.setRetailPrice(retailPrice);
        sku.setWholesalePrice(request.getWholesalePrice());
        sku.setBarcode(request.getBarcode() != null ? request.getBarcode().trim() : null);
        sku.setAttributeValues(request.getAttributeValues());

        return mapToResponse(skuRepository.save(sku));
    }

    // Xoa mem bien the SKU
    @Override
    @Transactional
    public void deleteSku(Long skuId) {
        Long tenantId = securityUtils.getCurrentTenantId();

        Sku sku = skuRepository.findByIdAndDeletedFalse(skuId)
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay bien the voi ID: " + skuId));

        if (sku.getTenant() == null || !sku.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("Khong tim thay bien the voi ID: " + skuId);
        }

        sku.setDeleted(true);
        skuRepository.save(sku);
    }

    // Chuyen doi Entity sang DTO
    private SkuResponse mapToResponse(Sku sku) {
        BigDecimal retail = sku.getRetailPrice() != null ? sku.getRetailPrice() : sku.getSellingPrice();
        BigDecimal selling = sku.getSellingPrice() != null ? sku.getSellingPrice() : sku.getRetailPrice();

        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .productId(sku.getProduct() != null ? sku.getProduct().getId() : null)
                .productName(sku.getProduct() != null ? sku.getProduct().getName() : null)
                .attributeValues(sku.getAttributeValues())
                .barcode(sku.getBarcode())
                .costPrice(sku.getCostPrice())
                .sellingPrice(selling)
                .retailPrice(retail)
                .wholesalePrice(sku.getWholesalePrice())
                .build();
    }
}
