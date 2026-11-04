package com.oism.web.branch;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.branch.BranchService;
import com.oism.application.branch.BranchView;
import com.oism.domain.model.BranchType;
import com.oism.web.common.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-AUTH-04: chi nhanh/kho. GET: moi vai tro; ghi: Owner. */
@RestController
@RequestMapping("/api/v1/branches")
@Tag(name = "Branches")
public class BranchController {

    private final BranchService branchService;

    public BranchController(BranchService branchService) {
        this.branchService = branchService;
    }

    public record BranchRequest(
            @NotBlank(message = "Tên chi nhánh không được để trống")
            @Size(max = 255, message = "Tên chi nhánh tối đa 255 ký tự")
            String name,
            @NotNull(message = "Loại chi nhánh không được để trống (STORE hoặc WAREHOUSE)")
            BranchType type,
            @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
            String address) {
    }

    @GetMapping
    @Operation(summary = "Danh sách chi nhánh của cửa hàng")
    public List<BranchView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return branchService.list(user.tenantId());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Tạo chi nhánh/kho (Owner)")
    public ResponseEntity<BranchView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                             @Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(branchService.create(user.tenantId(), request.name(), request.type(), request.address()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Sửa chi nhánh (Owner)")
    public BranchView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                             @Valid @RequestBody BranchRequest request) {
        return branchService.update(user.tenantId(), id, request.name(), request.type(), request.address());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Ẩn/hiện chi nhánh (Owner); không ẩn được khi còn tồn kho hoặc đơn chưa kết thúc")
    public BranchView setStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                @Valid @RequestBody StatusRequest request) {
        return branchService.setActive(user.tenantId(), id, request.active());
    }
}
