package com.oism.application.catalog;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import com.oism.domain.service.Ean13;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.UUID;

/** FR-PROD-02..04: SKU (ma duy nhat trong tenant), ma vach EAN-13/Code128, gia le va gia si. */
@Service
public class SkuService {

    private static final String EAN_PREFIX = "893"; // Viet Nam
    private static final int BARCODE_RETRIES = 20;

    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final SecureRandom random = new SecureRandom();

    public SkuService(SkuRepository skuRepository,
                      ProductRepository productRepository,
                      StockBalanceRepository stockBalanceRepository) {
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.stockBalanceRepository = stockBalanceRepository;
    }

    /** POST /products/{id}/skus: them bien the. San pham don chi co dung 1 SKU. */
    @Transactional
    public SkuView add(UUID tenantId, UUID productId, SkuInput input) {
        Product product = findProduct(tenantId, productId);
        if (!product.isHasVariants() && skuRepository.countByTenantIdAndProductId(tenantId, productId) >= 1) {
            throw new BusinessException(ErrorCode.PR40001, "Sản phẩm đơn chỉ có đúng 1 SKU");
        }
        return create(tenantId, productId, input);
    }

    /** Dung chung khi tao san pham kem nhieu SKU mot lan. */
    @Transactional
    public SkuView create(UUID tenantId, UUID productId, SkuInput input) {
        String code = input.code().trim();
        String barcode = blankToNull(input.barcode());
        assertPrices(input.retailPrice(), input.wholesalePrice());
        int reorder = input.reorderPoint() == null ? 0 : input.reorderPoint();
        if (reorder < 0) {
            throw new BusinessException(ErrorCode.PR40001, "Ngưỡng đặt hàng lại không được âm");
        }
        if (skuRepository.existsByTenantIdAndCode(tenantId, code)) {
            throw new BusinessException(ErrorCode.PR40901, "Mã SKU đã tồn tại: " + code);
        }
        if (barcode != null && skuRepository.existsByTenantIdAndBarcode(tenantId, barcode)) {
            throw new BusinessException(ErrorCode.PR40901, "Mã vạch đã tồn tại: " + barcode);
        }
        Sku sku = new Sku(tenantId, productId, code, input.attributes(), barcode,
                input.retailPrice(), input.wholesalePrice(), reorder);
        return SkuView.from(skuRepository.save(sku));
    }

    @Transactional
    public SkuView update(UUID tenantId, UUID skuId, Map<String, String> attributes, Integer reorderPoint,
                          CatalogStatus status) {
        Sku sku = find(tenantId, skuId);
        if (attributes != null) {
            sku.setAttributes(attributes);
        }
        if (reorderPoint != null) {
            if (reorderPoint < 0) {
                throw new BusinessException(ErrorCode.PR40001, "Ngưỡng đặt hàng lại không được âm");
            }
            sku.setReorderPoint(reorderPoint);
        }
        if (status != null) {
            sku.setStatus(status);
        }
        return SkuView.from(skuRepository.save(sku));
    }

    /** FR-PROD-04: gia > 0, tach biet voi gia von. */
    @Transactional
    public SkuView changePrices(UUID tenantId, UUID skuId, BigDecimal retailPrice, BigDecimal wholesalePrice) {
        assertPrices(retailPrice, wholesalePrice);
        Sku sku = find(tenantId, skuId);
        sku.changePrices(retailPrice, wholesalePrice);
        return SkuView.from(skuRepository.save(sku));
    }

    /** FR-PROD-03: tu sinh EAN-13 (co check digit), duy nhat trong tenant. */
    @Transactional
    public SkuView generateBarcode(UUID tenantId, UUID skuId) {
        Sku sku = find(tenantId, skuId);
        if (sku.getBarcode() != null) {
            throw new BusinessException(ErrorCode.PR40901, "SKU đã có mã vạch");
        }
        for (int i = 0; i < BARCODE_RETRIES; i++) {
            String candidate = Ean13.complete(EAN_PREFIX + String.format("%09d", random.nextInt(1_000_000_000)));
            if (!skuRepository.existsByTenantIdAndBarcode(tenantId, candidate)) {
                sku.setBarcode(candidate);
                return SkuView.from(skuRepository.save(sku));
            }
        }
        throw new BusinessException(ErrorCode.PR40901, "Không sinh được mã vạch duy nhất, thử lại");
    }

    /** GET /skus/lookup?barcode=: tra SKU cho POS kem hang kha dung (FR-POS-02). */
    @Transactional(readOnly = true)
    public SkuLookupView lookup(UUID tenantId, String barcode, UUID branchId) {
        Sku sku = skuRepository.findByTenantIdAndBarcode(tenantId, barcode.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU theo mã vạch"));
        String productName = productRepository.findByIdAndTenantId(sku.getProductId(), tenantId)
                .map(Product::getName).orElse(null);
        long available = stockBalanceRepository.sumAvailable(tenantId, sku.getId(), branchId);
        return new SkuLookupView(SkuView.from(sku), productName, available);
    }

    /**
     * GET /skus: tim SKU phang cho man hinh chon hang (phieu nhap, POS). keyword khop gan dung ma SKU hoac ten
     * san pham, khop chinh xac ma vach. Kem hang kha dung (tong cac chi nhanh hoac mot chi nhanh).
     */
    @Transactional(readOnly = true)
    public PageResult<SkuSearchView> search(UUID tenantId, String keyword, CatalogStatus status, UUID branchId,
                                            Integer page, Integer size) {
        Specification<Sku> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String kw = keyword.trim();
                String like = "%" + kw.toLowerCase() + "%";
                Subquery<UUID> byName = query.subquery(UUID.class);
                Root<Product> product = byName.from(Product.class);
                byName.select(product.get("id")).where(cb.like(cb.lower(product.get("name")), like));
                ps.add(cb.or(cb.like(cb.lower(root.get("code")), like), cb.equal(root.get("barcode"), kw),
                        root.get("productId").in(byName)));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<Sku> result = skuRepository.findAll(spec, Paging.of(page, size, Sort.by("code")));

        List<UUID> skuIds = result.getContent().stream().map(Sku::getId).toList();
        Map<UUID, String> productNames = productRepository.findAllById(
                        result.getContent().stream().map(Sku::getProductId).distinct().toList()).stream()
                .filter(p -> p.getTenantId().equals(tenantId))
                .collect(Collectors.toMap(Product::getId, Product::getName));
        Map<UUID, Long> available = skuIds.isEmpty() ? Map.of()
                : stockBalanceRepository.findByTenantIdAndSkuIdIn(tenantId, skuIds).stream()
                        .filter(b -> branchId == null || b.getBranchId().equals(branchId))
                        .collect(Collectors.groupingBy(StockBalance::getSkuId,
                                Collectors.summingLong(b -> (long) b.getAvailable())));
        return PageResult.of(result, s -> new SkuSearchView(s.getId(), s.getProductId(), productNames.get(s.getProductId()),
                s.getCode(), s.getAttributes(), s.getBarcode(), s.getRetailPrice(), s.getWholesalePrice(),
                s.getAvgCost(), s.getStatus(), available.getOrDefault(s.getId(), 0L)));
    }

    private Sku find(UUID tenantId, UUID skuId) {
        return skuRepository.findByIdAndTenantId(skuId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));
    }

    private Product findProduct(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy sản phẩm"));
    }

    private static void assertPrices(BigDecimal retail, BigDecimal wholesale) {
        if (retail == null || retail.signum() <= 0) {
            throw new BusinessException(ErrorCode.PR40001, "Giá bán lẻ phải lớn hơn 0");
        }
        if (wholesale != null && wholesale.signum() <= 0) {
            throw new BusinessException(ErrorCode.PR40001, "Giá bán sỉ phải lớn hơn 0");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
