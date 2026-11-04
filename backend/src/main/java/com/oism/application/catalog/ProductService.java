package com.oism.application.catalog;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BrandRepository;
import com.oism.domain.repository.CategoryRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SkuRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** FR-PROD-02, FR-PROD-05: san pham don / co bien the, tim kiem, loc, phan trang. */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final SkuService skuService;

    public ProductService(ProductRepository productRepository,
                          SkuRepository skuRepository,
                          CategoryRepository categoryRepository,
                          BrandRepository brandRepository,
                          SkuService skuService) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.skuService = skuService;
    }

    /** Tao san pham kem toan bo SKU trong mot giao dich. San pham don phai co dung 1 SKU. */
    @Transactional
    public ProductView create(UUID tenantId, String name, String description, UUID categoryId, UUID brandId,
                              boolean hasVariants, List<SkuInput> skus) {
        if (skus == null || skus.isEmpty()) {
            throw new BusinessException(ErrorCode.PR40001, "Sản phẩm phải có ít nhất 1 SKU");
        }
        if (!hasVariants && skus.size() != 1) {
            throw new BusinessException(ErrorCode.PR40001, "Sản phẩm đơn phải có đúng 1 SKU");
        }
        assertDistinctCodes(skus);
        assertCategoryAndBrand(tenantId, categoryId, brandId);

        Product product = productRepository.save(new Product(tenantId, categoryId, brandId, name.trim(),
                blankToNull(description), hasVariants));
        List<SkuView> created = skus.stream().map(s -> skuService.create(tenantId, product.getId(), s)).toList();
        return ProductView.from(product, created);
    }

    @Transactional
    public ProductView update(UUID tenantId, UUID id, String name, String description, UUID categoryId, UUID brandId) {
        Product product = find(tenantId, id);
        assertCategoryAndBrand(tenantId, categoryId, brandId);
        product.setName(name.trim());
        product.setDescription(blankToNull(description));
        product.setCategoryId(categoryId);
        product.setBrandId(brandId);
        productRepository.save(product);
        return view(tenantId, product);
    }

    @Transactional
    public ProductView setStatus(UUID tenantId, UUID id, CatalogStatus status) {
        Product product = find(tenantId, id);
        product.setStatus(status);
        productRepository.save(product);
        return view(tenantId, product);
    }

    @Transactional(readOnly = true)
    public ProductView get(UUID tenantId, UUID id) {
        return view(tenantId, find(tenantId, id));
    }

    /** Tim theo ten/SKU (gan dung) va ma vach (khop chinh xac); loc danh muc, thuong hieu, trang thai. */
    @Transactional(readOnly = true)
    public PageResult<ProductSummary> search(UUID tenantId, String keyword, UUID categoryId, UUID brandId,
                                             CatalogStatus status, Integer page, Integer size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (brandId != null) {
                predicates.add(cb.equal(root.get("brandId"), brandId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim();
                String like = "%" + kw.toLowerCase() + "%";
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<Sku> sku = sub.from(Sku.class);
                sub.select(sku.get("id")).where(
                        cb.equal(sku.get("productId"), root.get("id")),
                        cb.or(cb.like(cb.lower(sku.get("code")), like), cb.equal(sku.get("barcode"), kw)));
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.exists(sub)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> result = productRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Set<UUID> ids = result.getContent().stream().map(Product::getId).collect(Collectors.toSet());
        Map<UUID, List<Sku>> skusByProduct = ids.isEmpty() ? Map.of()
                : skuRepository.findByTenantIdAndProductIdIn(tenantId, ids).stream()
                        .collect(Collectors.groupingBy(Sku::getProductId));

        return PageResult.of(result, p -> summarize(p, skusByProduct.getOrDefault(p.getId(), List.of())));
    }

    private ProductSummary summarize(Product p, List<Sku> skus) {
        BigDecimal min = skus.stream().map(Sku::getRetailPrice).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal max = skus.stream().map(Sku::getRetailPrice).max(Comparator.naturalOrder()).orElse(null);
        return new ProductSummary(p.getId(), p.getName(), p.getCategoryId(), p.getBrandId(), p.isHasVariants(),
                p.getStatus(), skus.size(), min, max);
    }

    private ProductView view(UUID tenantId, Product product) {
        List<SkuView> skus = skuRepository.findByTenantIdAndProductIdOrderByCodeAsc(tenantId, product.getId())
                .stream().map(SkuView::from).toList();
        return ProductView.from(product, skus);
    }

    private Product find(UUID tenantId, UUID id) {
        return productRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy sản phẩm"));
    }

    private void assertCategoryAndBrand(UUID tenantId, UUID categoryId, UUID brandId) {
        if (categoryId != null && categoryRepository.findByIdAndTenantId(categoryId, tenantId).isEmpty()) {
            throw new BusinessException(ErrorCode.PR40401, "Không tìm thấy danh mục");
        }
        if (brandId != null && brandRepository.findByIdAndTenantId(brandId, tenantId).isEmpty()) {
            throw new BusinessException(ErrorCode.PR40401, "Không tìm thấy thương hiệu");
        }
    }

    private static void assertDistinctCodes(List<SkuInput> skus) {
        Set<String> codes = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        for (SkuInput s : skus) {
            if (!codes.add(s.code().trim())) {
                throw new BusinessException(ErrorCode.PR40901, "Trùng mã SKU trong cùng yêu cầu: " + s.code());
            }
            if (s.barcode() != null && !s.barcode().isBlank() && !barcodes.add(s.barcode().trim())) {
                throw new BusinessException(ErrorCode.PR40901, "Trùng mã vạch trong cùng yêu cầu: " + s.barcode());
            }
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
