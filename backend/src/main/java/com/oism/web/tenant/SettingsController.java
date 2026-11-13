package com.oism.web.tenant;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.order.ChannelCredentialService;
import com.oism.application.tenant.TenantSettingsService;
import com.oism.domain.model.OrderChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

@Tag(name = "Cài đặt hệ thống", description = "JC-31, JC-32: Cài đặt thời hạn giữ hàng và thông số tenant")
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final TenantSettingsService tenantSettingsService;
    private final ChannelCredentialService channelCredentialService;

    public SettingsController(TenantSettingsService tenantSettingsService,
                              ChannelCredentialService channelCredentialService) {
        this.tenantSettingsService = tenantSettingsService;
        this.channelCredentialService = channelCredentialService;
    }

    public record UpdateSettingsRequest(
            @Size(max = 150, message = "Tên cửa hàng tối đa 150 ký tự")
            String storeName,
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
         return tenantSettingsService.updateSettings(user.tenantId(), request.storeName(), request.reservationMinutes());
    }

    @Operation(summary = "(bổ sung FR-SIM-01) Trạng thái API key webhook của từng sàn - Owner. Không trả về key")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/channel-keys")
    public List<ChannelCredentialService.ChannelKeyView> listChannelKeys(@AuthenticationPrincipal AuthenticatedUser user) {
        return channelCredentialService.list(user.tenantId());
    }

    @Operation(summary = "(bổ sung FR-SIM-01) Sinh API key webhook mới cho sàn SHOPEE/TIKTOK - Owner. "
            + "Key cũ của sàn đó hết hiệu lực; key mới chỉ hiển thị một lần. Lỗi: 400 OD40001 sàn không hỗ trợ")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/channel-keys/{channel}")
    public ResponseEntity<ChannelCredentialService.IssuedKey> issueChannelKey(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable OrderChannel channel) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelCredentialService.issue(user.tenantId(), channel));
    }
}
