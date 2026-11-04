package com.oism.web.catalog;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.catalog.ProductService;
import com.oism.application.catalog.ProductSummary;
import com.oism.application.catalog.ProductView;
import com.oism.application.catalog.SkuInput;
import com.oism.application.catalog.SkuService;
import com.oism.application.catalog.SkuView;
import com.oism.application.common.PageResult;
import com.oism.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-PROD-02, FR-PROD-05: san pham (don hoac nhieu bien the), tim kiem, loc, phan trang. */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products")
public class ProductController {

    /** Quy tac ten san pham cua de bai: chu cai hoa dau, tiep theo 2-50 ky tu chu/so/khoang trang. */
    static final String NAME_PATTERN = "^[A-Z][a-zA-Z0-9\\s]{2,50}$";
    static final String NAME_MESSAGE = "Tên sản phẩm phải bắt đầu bằng chữ in hoa, gồm 3-51 ký tự chữ không dấu, số hoặc khoảng trắng";

    private final ProductService productService;
    private final SkuService skuService;

    public ProductController(ProductService productService, SkuService skuService) {
        this.productService = productService;
        this.skuService = skuService;
    }

    public record CreateProductRequest(
            @NotBlank(message = "Tên sản phẩm không được để trống")
            @Pattern(regexp = NAME_PATTERN, message = NAME_MESSAGE)
            String name,
            @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
            String description,
            UUID categoryId,
            UUID brandId,
            boolean hasVariants,
            @NotEmpty(message = "Sản phẩm phải có ít nhất 1 SKU")
            @Valid List<SkuRequest> skus) {
    }

    public record UpdateProductRequest(
            @NotBlank(message = "Tên sản phẩm không được để trống")
            @Pattern(regexp = NAME_PATTERN, message = NAME_MESSAGE)
            String name,
            @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
            String description,
            UUID categoryId,
            UUID brandId) {
    }

    public record ProductStatusRequest(@NotNull(message = "status không được để trống (ACTIVE hoặc INACTIVE)")
                                       CatalogStatus status) {
    }

    @GetMapping
    @Operation(summary = "Tìm kiếm, lọc, phân trang sản phẩm (keyword khớp gần đúng tên/mã SKU và khớp chính xác mã vạch)")
    public PageResult<ProductSummary> search(@AuthenticationPrincipal AuthenticatedUser user,
                                             @RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) UUID categoryId,
                                             @RequestParam(required = false) UUID brandId,
                                             @RequestParam(required = false) CatalogStatus status,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return productService.search(user.tenantId(), keyword, categoryId, brandId, status, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết sản phẩm kèm danh sách SKU")
    public ProductView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return productService.get(user.tenantId(), id);
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Tạo sản phẩm kèm toàn bộ SKU biến thể trong một lần (Owner)")
    public ResponseEntity<ProductView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                              @Valid @RequestBody CreateProductRequest r) {
        List<SkuInput> skus = r.skus().stream().map(SkuRequest::toInput).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(
                user.tenantId(), r.name(), r.description(), r.categoryId(), r.brandId(), r.hasVariants(), skus));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Sửa sản phẩm (Owner)")
    public ProductView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                              @Valid @RequestBody UpdateProductRequest r) {
        return productService.update(user.tenantId(), id, r.name(), r.description(), r.categoryId(), r.brandId());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Ngừng bán / bán lại sản phẩm (Owner)")
    public ProductView setStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                 @Valid @RequestBody ProductStatusRequest r) {
        return productService.setStatus(user.tenantId(), id, r.status());
    }

    @PostMapping("/{id}/skus")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Thêm một SKU biến thể vào sản phẩm (Owner)")
    public ResponseEntity<SkuView> addSku(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                          @Valid @RequestBody SkuRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(skuService.add(user.tenantId(), id, r.toInput()));
    }
}
