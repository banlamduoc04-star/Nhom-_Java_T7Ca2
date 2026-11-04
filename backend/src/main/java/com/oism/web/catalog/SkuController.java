package com.oism.web.catalog;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.catalog.SkuLookupView;
import com.oism.application.catalog.SkuSearchView;
import com.oism.application.common.PageResult;
import com.oism.application.catalog.SkuService;
import com.oism.application.catalog.SkuView;
import com.oism.domain.model.CatalogStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** FR-PROD-02..04: SKU, gia, ma vach, tra cuu cho POS. */
@RestController
@RequestMapping("/api/v1/skus")
@Tag(name = "SKUs")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    public record UpdateSkuRequest(Map<String, String> attributes,
                                   @Min(value = 0, message = "Ngưỡng đặt hàng lại không được âm") Integer reorderPoint,
                                   CatalogStatus status) {
    }

    public record PriceRequest(
            @NotNull(message = "Giá bán lẻ không được để trống")
            @DecimalMin(value = "0.01", message = "Giá bán lẻ phải lớn hơn 0")
            BigDecimal retailPrice,
            @DecimalMin(value = "0.01", message = "Giá bán sỉ phải lớn hơn 0")
            BigDecimal wholesalePrice) {
    }

    @GetMapping
    @Operation(summary = "Tìm SKU phẳng theo mã SKU/tên sản phẩm (gần đúng) hoặc mã vạch (chính xác), kèm hàng khả dụng")
    public PageResult<SkuSearchView> search(@AuthenticationPrincipal AuthenticatedUser user,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) CatalogStatus status,
                                            @RequestParam(required = false) UUID branchId,
                                            @RequestParam(required = false) Integer page,
                                            @RequestParam(required = false) Integer size) {
        return skuService.search(user.tenantId(), keyword, status, branchId, page, size);
    }

    @GetMapping("/lookup")
    @Operation(summary = "Tra SKU theo mã vạch kèm hàng khả dụng (POS)")
    public SkuLookupView lookup(@AuthenticationPrincipal AuthenticatedUser user,
                                @RequestParam String barcode,
                                @RequestParam(required = false) UUID branchId) {
        return skuService.lookup(user.tenantId(), barcode, branchId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Sửa thuộc tính, ngưỡng đặt hàng lại, ngừng bán SKU (Owner)")
    public SkuView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                          @Valid @RequestBody UpdateSkuRequest r) {
        return skuService.update(user.tenantId(), id, r.attributes(), r.reorderPoint(), r.status());
    }

    @PutMapping("/{id}/prices")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Đổi giá bán lẻ/sỉ (Owner); không ảnh hưởng đơn đã tạo")
    public SkuView changePrices(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                @Valid @RequestBody PriceRequest r) {
        return skuService.changePrices(user.tenantId(), id, r.retailPrice(), r.wholesalePrice());
    }

    @PostMapping("/{id}/barcode")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @Operation(summary = "Sinh mã vạch EAN-13 có check digit (Owner, Staff)")
    public SkuView generateBarcode(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return skuService.generateBarcode(user.tenantId(), id);
    }
}
