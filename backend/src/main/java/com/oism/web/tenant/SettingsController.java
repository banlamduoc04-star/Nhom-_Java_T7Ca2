package com.oism.web.tenant;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.tenant.TenantSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cài đặt hệ thống", description = "JC-31, JC-32: Cài đặt thời hạn giữ hàng và thông số tenant")
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final TenantSettingsService tenantSettingsService;

    public SettingsController(TenantSettingsService tenantSettingsService) {
        this.tenantSettingsService = tenantSettingsService;
    }

    public record UpdateSettingsRequest(
            @NotNull(message = "Thời gian giữ hàng không được để trống")
            @Min(value = 1, message = "Thời gian giữ hàng tối thiểu 1 phút")
            @Max(value = 1440, message = "Thời gian giữ hàng tối đa 1440 phút (24 giờ)")
            Integer reservationMinutes
    ) {}

    @Operation(summary = "Xem cài đặt hiện tại của tenant")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public TenantSettingsService.TenantSettingsView getSettings(@AuthenticationPrincipal AuthenticatedUser user) {
        return tenantSettingsService.getSettings(user.tenantId());
    }

    @Operation(summary = "Cập nhật thời hạn giữ hàng cho tenant")
    @PreAuthorize("hasRole('OWNER')")
    @PutMapping
    public TenantSettingsService.TenantSettingsView updateSettings(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateSettingsRequest request) {
        return tenantSettingsService.updateSettings(user.tenantId(), request.reservationMinutes());
    }
}

