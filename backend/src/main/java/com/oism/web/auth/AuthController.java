package com.oism.web.auth;

import com.oism.application.auth.AuthService;
import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.auth.LoginCommand;
import com.oism.application.auth.RegisterCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint AUTH (Ke hoach muc 4): register, login, refresh, logout, me. Chi goi AuthService. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

     @PostMapping({"/auth/register", "/auth/register-tenant"})
    @SecurityRequirements
    @Operation(summary = "Đăng ký cửa hàng (tạo tenant + Owner + chi nhánh mặc định)")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        var result = authService.register(new RegisterCommand(
                request.storeName(), request.fullName(), request.email(), request.phone(), request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(RegisterResponse.from(result));
    }

    @PostMapping("/auth/login")
    @SecurityRequirements
    @Operation(summary = "Đăng nhập bằng email hoặc số điện thoại")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return TokenResponse.from(authService.login(new LoginCommand(request.identifier(), request.password())));
    }

    @PostMapping("/auth/refresh")
    @SecurityRequirements
    @Operation(summary = "Cấp lại access token (refresh token xoay vòng)")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return TokenResponse.from(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/auth/logout")
    @Operation(summary = "Đăng xuất, thu hồi refresh token")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody RefreshRequest request) {
        authService.logout(user, request.refreshToken());
        return ResponseEntity.noContent().build();
    }

     @GetMapping("/me")
    @Operation(summary = "Thông tin người đang đăng nhập")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return MeResponse.from(authService.me(user));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Đổi mật khẩu của chính mình, thu hồi mọi phiên đăng nhập")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
