package com.oism.web.order;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.common.PageResult;
import com.oism.application.order.OrderLine;
import com.oism.application.order.OrderService;
import com.oism.application.order.OrderView;
import com.oism.application.order.ShippingSlipService;
import com.oism.application.order.ShippingSlipView;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** FR-ORD-01..03 (Sprint 2): tao don tay (Draft), danh sach, chi tiet kem lich su. Owner, Staff. */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService orderService;
    private final ShippingSlipService shippingSlipService;

    public OrderController(OrderService orderService, ShippingSlipService shippingSlipService) {
        this.orderService = orderService;
        this.shippingSlipService = shippingSlipService;
    }

    public record ItemRequest(
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 1, message = "Số lượng phải lớn hơn 0") int quantity) {
    }

    public record CreateOrderRequest(
            @NotNull(message = "Chi nhánh không được để trống") UUID branchId,
            @Size(max = 255, message = "Tên khách tối đa 255 ký tự") String customerName,
            @Pattern(regexp = "^$|^[0-9+]{8,15}$", message = "Số điện thoại không hợp lệ") String customerPhone,
            @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự") String shippingAddress,
            @NotEmpty(message = "Đơn hàng cần ít nhất 1 dòng hàng") @Valid List<ItemRequest> items) {
    }

    @GetMapping
    @Operation(summary = "Danh sách đơn, lọc theo trạng thái/kênh/chi nhánh/khoảng ngày, phân trang")
    public PageResult<OrderView> list(@AuthenticationPrincipal AuthenticatedUser user,
                                      @RequestParam(required = false) OrderStatus status,
                                      @RequestParam(required = false) OrderChannel channel,
                                      @RequestParam(required = false) UUID branchId,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                      @RequestParam(required = false)
                                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                      @RequestParam(required = false) Integer page,
                                      @RequestParam(required = false) Integer size) {
        return orderService.list(user.tenantId(), status, channel, branchId, from, to, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết đơn kèm dòng hàng và lịch sử trạng thái")
    public OrderView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderService.get(user.tenantId(), id);
    }

    @GetMapping("/{id}/shipping-slip")
    @Operation(summary = "Dữ liệu in phiếu giao hàng (FR-ORD-03) - Owner, Staff. Lỗi: 404 OD40401, 409 OD40901 đơn đã hủy")
    public ShippingSlipView shippingSlip(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return shippingSlipService.shippingSlip(user.tenantId(), id);
    }

    @PostMapping
    @Operation(summary = "Tạo đơn tay (Draft); đơn giá lấy theo giá bán lẻ hiện tại của SKU")
    public ResponseEntity<OrderView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                            @Valid @RequestBody CreateOrderRequest r) {
        List<OrderLine> lines = r.items().stream().map(i -> new OrderLine(i.skuId(), i.quantity())).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createDraft(
                user.tenantId(), user.userId(), r.branchId(), r.customerName(), r.customerPhone(),
                r.shippingAddress(), lines));
    }

    public record CancelOrderRequest(
            @NotNull(message = "Lý do hủy đơn không được để trống")
            @Size(min = 1, max = 500, message = "Lý do hủy từ 1 đến 500 ký tự")
            String reason) {
    }

    @PostMapping("/{id}/reserve")
    @Operation(summary = "Giữ hàng cho đơn (Draft -> Reserved); khóa FOR UPDATE theo thứ tự SKU")
    public OrderView reserve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderService.reserve(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Duyệt đơn và trừ kho (Reserved -> Confirmed); chụp giá vốn cost_price và ghi sổ cái")
    public OrderView confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderService.confirm(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Hoàn tất đơn hàng (Confirmed -> Completed)")
    public OrderView complete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return orderService.complete(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy đơn hàng (Draft/Reserved/Confirmed -> Cancelled); bắt buộc nhập lý do, tự động nhả/hoàn hàng")
    public OrderView cancel(@AuthenticationPrincipal AuthenticatedUser user,
                            @PathVariable UUID id,
                            @Valid @RequestBody CancelOrderRequest r) {
        return orderService.cancel(user.tenantId(), user.userId(), id, r.reason());
    }
}
