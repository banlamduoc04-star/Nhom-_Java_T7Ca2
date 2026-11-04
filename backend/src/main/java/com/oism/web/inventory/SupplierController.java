package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.inventory.SupplierService;
import com.oism.application.inventory.SupplierView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-INV-05: nha cung cap. Owner, Staff. */
@RestController
@RequestMapping("/api/v1/suppliers")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    public record SupplierRequest(
            @NotBlank(message = "Tên nhà cung cấp không được để trống")
            @Size(max = 255, message = "Tên nhà cung cấp tối đa 255 ký tự")
            String name,
            @Pattern(regexp = "^$|^[0-9+]{8,15}$", message = "Số điện thoại không hợp lệ")
            String phone,
            @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
            String address,
            Boolean active) {
    }

    @GetMapping
    @Operation(summary = "Danh sách nhà cung cấp")
    public List<SupplierView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return supplierService.list(user.tenantId());
    }

    @PostMapping
    @Operation(summary = "Thêm nhà cung cấp")
    public ResponseEntity<SupplierView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody SupplierRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.create(user.tenantId(), r.name(), r.phone(), r.address()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa / ngừng dùng nhà cung cấp (active=false)")
    public SupplierView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                               @Valid @RequestBody SupplierRequest r) {
        return supplierService.update(user.tenantId(), id, r.name(), r.phone(), r.address(),
                r.active() == null || r.active());
    }
}
