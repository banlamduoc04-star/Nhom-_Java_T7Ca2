package com.oism.web.user;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.user.UserService;
import com.oism.application.user.UserView;
import com.oism.domain.model.Role;
import com.oism.web.common.StatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-AUTH-05: Owner quan ly tai khoan Staff/Cashier. */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public record CreateUserRequest(
            @NotBlank(message = "Email không được để trống")
            @Email(message = "Email không đúng định dạng")
            @Size(max = 255, message = "Email tối đa 255 ký tự")
            String email,
            @Pattern(regexp = "^$|^[0-9+]{8,15}$", message = "Số điện thoại không hợp lệ")
            String phone,
            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
            String password,
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
            String fullName,
            @NotNull(message = "Vai trò không được để trống (STAFF hoặc CASHIER)")
            Role role,
            UUID branchId) {
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
            String fullName,
            @Pattern(regexp = "^$|^[0-9+]{8,15}$", message = "Số điện thoại không hợp lệ")
            String phone,
            @NotNull(message = "Vai trò không được để trống (STAFF hoặc CASHIER)")
            Role role,
            UUID branchId) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, max = 72, message = "Mật khẩu từ 8 đến 72 ký tự")
            String newPassword) {
    }

    @GetMapping
    @Operation(summary = "Danh sách tài khoản của cửa hàng (Owner)")
    public List<UserView> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return userService.list(user.tenantId());
    }

    @PostMapping
    @Operation(summary = "Tạo tài khoản Staff/Cashier (Owner)")
    public ResponseEntity<UserView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                           @Valid @RequestBody CreateUserRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(
                user.tenantId(), r.email(), r.phone(), r.password(), r.fullName(), r.role(), r.branchId()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa thông tin, vai trò, chi nhánh của nhân viên (Owner)")
    public UserView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                           @Valid @RequestBody UpdateUserRequest r) {
        return userService.update(user.tenantId(), id, r.fullName(), r.phone(), r.role(), r.branchId());
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Khóa/mở khóa tài khoản (Owner)")
    public UserView setStatus(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                              @Valid @RequestBody StatusRequest r) {
        return userService.setActive(user, id, r.active());
    }

    @PutMapping("/{id}/password")
    @Operation(summary = "Đặt lại mật khẩu cho nhân viên (Owner)")
    public ResponseEntity<Void> resetPassword(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                                              @Valid @RequestBody ResetPasswordRequest r) {
        userService.resetPassword(user.tenantId(), id, r.newPassword());
        return ResponseEntity.noContent().build();
    }
}
