package com.oism.web.catalog;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.catalog.BrandService;
import com.oism.application.catalog.BrandView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-PROD-01: thuong hieu. GET: moi vai tro; ghi: Owner. */
@RestController
@RequestMapping("/api/v1/brands")
@Tag(name = "Brands")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    public record BrandRequest(
            @NotBlank(message = "Tên thương hiệu không được để trống")
            @Size(max = 150, message = "Tên thương hiệu tối đa 150 ký tự")
            String name) {
    }

    @GetMapping
    @Operation(summary = "Danh sách thương hiệu")
    public List<BrandView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return brandService.list(user.tenantId());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Tạo thương hiệu (Owner)")
    public ResponseEntity<BrandView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody BrandRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(user.tenantId(), r.name()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Sửa thương hiệu (Owner)")
    public BrandView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                            @Valid @RequestBody BrandRequest r) {
        return brandService.update(user.tenantId(), id, r.name());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Xóa thương hiệu (Owner); 409 PR40902 nếu còn sản phẩm")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        brandService.delete(user.tenantId(), id);
        return ResponseEntity.noContent().build();
    }
}
