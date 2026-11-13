package com.oism.web.order;

import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.application.order.OrderView;
import com.oism.application.order.WebhookCaller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Webhook nhan don tu san TMDT (FR-ORD-01, FR-SIM-01): POST /webhooks/{channel}, header X-Api-Key.
 * Tenant va san lay tu API key (WebhookApiKeyFilter), khong lay tu request. Don duoc tao va giu hang ngay.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "FR-SIM-01: nhận đơn từ sàn, xác thực bằng API key của tenant (header X-Api-Key)")
public class WebhookController {

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
            @NotBlank(message = "Mã đơn ngoài không được để trống")
            @Size(max = 100, message = "Mã đơn ngoài tối đa 100 ký tự") String externalOrderId,
            @Size(max = 255, message = "Tên khách hàng tối đa 255 ký tự") String customerName,
            @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự") String customerPhone,
            @Size(max = 500, message = "Địa chỉ giao hàng tối đa 500 ký tự") String shippingAddress,
            @NotEmpty(message = "Cần ít nhất 1 dòng hàng") List<@Valid @NotNull WebhookItem> items) {
    }

    @PostMapping("/{channel}")
    @Operation(summary = "Nhận đơn từ sàn SHOPEE/TIKTOK, tạo và giữ hàng (header X-Api-Key). "
            + "Lỗi: 401 OD40101 key sai, 400 OD40001, 409 OD40902 đơn trùng, 409 IV40901 hết hàng",
            parameters = @Parameter(name = "X-Api-Key", in = ParameterIn.HEADER, required = true,
                    description = "API key webhook của tenant cho đúng sàn (Owner sinh ở Cài đặt)"))
    public ResponseEntity<OrderView> receiveWebhook(@AuthenticationPrincipal WebhookCaller caller,
                                                    @Valid @RequestBody WebhookOrderRequest req) {
        List<OrderLine> lines = req.items().stream()
                .map(i -> new OrderLine(i.skuId(), i.quantity()))
                .toList();
        OrderView view = orderService.createAndReserve(
                caller.tenantId(), null, req.branchId(), caller.channel(), req.externalOrderId(),
                req.customerName(), req.customerPhone(), req.shippingAddress(), lines);
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }
}
