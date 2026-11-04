package com.oism.web.order;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.application.order.OrderView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Webhook nhan don hang tu san TMDT (Shopee, TikTok Shop) - FR-ORD-01, FR-RSE-01.
 * Tu dong giu hang (Draft -> Reserved) de chong ban vuot ngay khi don roi vao he thong.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks")
public class WebhookController {

    private static final String DEFAULT_WEBHOOK_KEY = "oism-webhook-secret-key";

    private final OrderService orderService;

    public WebhookController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record WebhookItem(
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 1, message = "Số lượng phải lớn hơn 0") int quantity) {
    }

    public record WebhookOrderRequest(
            @NotNull(message = "Chi nhánh không được để trống") UUID branchId,
            @NotNull(message = "Mã đơn ngoài không được để trống") @Size(min = 1, max = 100) String externalOrderId,
            String customerName,
            String customerPhone,
            String shippingAddress,
            @NotEmpty(message = "Cần ít nhất 1 dòng hàng") @Valid List<WebhookItem> items) {
    }

    @PostMapping("/{channel}")
    @Operation(summary = "Tiếp nhận đơn webhook từ sàn (Shopee / TikTok Shop), tự động tạo và giữ hàng")
    public ResponseEntity<OrderView> receiveWebhook(
            @PathVariable String channel,
            @RequestHeader(value = "X-Webhook-Key", required = false) String webhookKey,
            @RequestHeader(value = "X-Tenant-Id", required = false) UUID headerTenantId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody WebhookOrderRequest req) {

        // Xac thuc webhook key
        if (webhookKey != null && !webhookKey.isBlank() && !DEFAULT_WEBHOOK_KEY.equals(webhookKey)) {
            throw new BusinessException(ErrorCode.OD40101, "API key webhook không hợp lệ");
        }

        UUID tenantId = user != null ? user.tenantId() : headerTenantId;
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.AU40101, "Thiếu thông tin tenant");
        }

        OrderChannel orderChannel;
        try {
            orderChannel = OrderChannel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.OD40001, "Kênh đơn hàng không hợp lệ: " + channel);
        }

        List<OrderLine> lines = req.items().stream()
                .map(i -> new OrderLine(i.skuId(), i.quantity()))
                .toList();

        OrderView view = orderService.createAndReserve(
                tenantId, null, req.branchId(), orderChannel, req.externalOrderId(),
                req.customerName(), req.customerPhone(), req.shippingAddress(), lines);

        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }
}
