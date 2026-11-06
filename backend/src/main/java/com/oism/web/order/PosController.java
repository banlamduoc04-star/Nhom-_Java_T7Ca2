package com.oism.web.order;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.order.PosCheckoutCommand;
import com.oism.application.order.PosCheckoutResponse;
import com.oism.application.order.PosCheckoutService;
import com.oism.domain.model.PaymentMethod;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Tag(name = "POS Checkout", description = "FR-POS-04, JC-31: Thanh toán POS trong một giao dịch cơ sở dữ liệu duy nhất")
@RestController
@RequestMapping("/api/v1/pos")
public class PosController {

    private final PosCheckoutService posCheckoutService;

    public PosController(PosCheckoutService posCheckoutService) {
        this.posCheckoutService = posCheckoutService;
    }

    public record PosCheckoutRequest(
            @NotNull(message = "Chi nhánh không được để trống")
            @Schema(description = "ID chi nhánh bán hàng", example = "a0000000-0000-0000-0000-000000000001")
            UUID branchId,

            @NotEmpty(message = "Giỏ hàng cần ít nhất 1 sản phẩm")
            List<PosItemRequest> items,

            @NotNull(message = "Phương thức thanh toán không được để trống")
            @Schema(description = "Phương thức thanh toán: CASH hoặc QR", example = "CASH")
            PaymentMethod paymentMethod,

            @Schema(description = "Tiền khách đưa (nếu là tiền mặt)", example = "500000")
            BigDecimal cashReceived,

            @Schema(description = "Tên khách hàng", example = "Nguyễn Văn A")
            String customerName,

            @Schema(description = "Số điện thoại khách hàng", example = "0901234567")
            String customerPhone,

            @Schema(description = "Ghi chú đơn hàng", example = "Khách lấy hóa đơn VAT")
            String note
    ) {}

    public record PosItemRequest(
            @NotNull(message = "SKU ID không được để trống")
            UUID skuId,

            @NotNull(message = "Số lượng không được để trống")
            Integer quantity
    ) {}

    @Operation(summary = "Thanh toán đơn hàng POS trong 1 giao dịch (Cashier, Staff, Owner)")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF', 'CASHIER')")
    @PostMapping("/checkout")
    public PosCheckoutResponse checkout(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PosCheckoutRequest req) {
        PosCheckoutCommand cmd = new PosCheckoutCommand(
                req.branchId(),
                req.items().stream().map(i -> new PosCheckoutCommand.PosLine(i.skuId(), i.quantity())).toList(),
                req.paymentMethod(),
                req.cashReceived(),
                req.customerName(),
                req.customerPhone(),
                req.note()
        );
        return posCheckoutService.checkout(user.tenantId(), user.userId(), user.email(), cmd);
    }
}
