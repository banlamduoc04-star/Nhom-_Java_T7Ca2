package com.oism.application.catalog;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Brand;
import com.oism.domain.repository.BrandRepository;
import com.oism.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** FR-PROD-01: thuong hieu. Ten duy nhat trong tenant; khong xoa duoc khi con san pham (PR40902). */
@Service
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandService(BrandRepository brandRepository, ProductRepository productRepository) {
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<BrandView> list(UUID tenantId) {
        return brandRepository.findByTenantIdOrderByNameAsc(tenantId).stream().map(BrandView::from).toList();
    }

    @Transactional
    public BrandView create(UUID tenantId, String name) {
        String cleanName = name.trim();
        if (brandRepository.existsByTenantIdAndNameIgnoreCase(tenantId, cleanName)) {
            throw new BusinessException(ErrorCode.PR40901, "Tên thương hiệu đã tồn tại");
        }
        return BrandView.from(brandRepository.save(new Brand(tenantId, cleanName)));
    }

    @Transactional
    public BrandView update(UUID tenantId, UUID id, String name) {
        Brand brand = find(tenantId, id);
        String cleanName = name.trim();
        if (!brand.getName().equalsIgnoreCase(cleanName)
                && brandRepository.existsByTenantIdAndNameIgnoreCase(tenantId, cleanName)) {
            throw new BusinessException(ErrorCode.PR40901, "Tên thương hiệu đã tồn tại");
        }
        brand.setName(cleanName);
        return BrandView.from(brandRepository.save(brand));
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        Brand brand = find(tenantId, id);
        if (productRepository.existsByTenantIdAndBrandId(tenantId, id)) {
            throw new BusinessException(ErrorCode.PR40902, "Thương hiệu còn sản phẩm");
        }
        brandRepository.delete(brand);
    }

    private Brand find(UUID tenantId, UUID id) {
        return brandRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy thương hiệu"));
    }
}
