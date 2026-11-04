package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.common.PageResult;
import com.oism.application.inventory.PurchaseReceiptService;
import com.oism.application.inventory.ReceiptLine;
import com.oism.application.inventory.ReceiptView;
import com.oism.domain.model.ReceiptStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** FR-INV-02, FR-COST-01: phieu nhap kho. Owner, Staff. */
@RestController
@RequestMapping("/api/v1/purchase-receipts")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Purchase receipts")
public class PurchaseReceiptController {

    private final PurchaseReceiptService receiptService;

    public PurchaseReceiptController(PurchaseReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    public record ItemRequest(
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 1, message = "Số lượng phải lớn hơn 0") int quantity,
            @NotNull(message = "Đơn giá không được để trống")
            @DecimalMin(value = "0", message = "Đơn giá không được âm")
            BigDecimal unitPrice) {

        ReceiptLine toLine() {
            return new ReceiptLine(skuId, quantity, unitPrice);
        }
    }

    public record ReceiptRequest(
            @NotNull(message = "Chi nhánh nhập không được để trống") UUID branchId,
            UUID supplierId,
            @NotEmpty(message = "Phiếu nhập cần ít nhất 1 dòng hàng")
            @Valid List<ItemRequest> items) {
    }

    @GetMapping
    @Operation(summary = "Danh sách phiếu nhập, lọc theo trạng thái/chi nhánh, phân trang")
    public PageResult<ReceiptView> list(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam(required = false) ReceiptStatus status,
                                        @RequestParam(required = false) UUID branchId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return receiptService.list(user.tenantId(), status, branchId, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu nhập")
    public ReceiptView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return receiptService.get(user.tenantId(), id);
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu nhập (trạng thái Nháp)")
    public ResponseEntity<ReceiptView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                              @Valid @RequestBody ReceiptRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(receiptService.create(
                user.tenantId(), user.userId(), r.branchId(), r.supplierId(),
                r.items().stream().map(ItemRequest::toLine).toList()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Sửa phiếu nhập khi còn Nháp; đã xác nhận → 409 IV40902")
    public ReceiptView update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id,
                              @Valid @RequestBody ReceiptRequest r) {
        return receiptService.update(user.tenantId(), id, r.branchId(), r.supplierId(),
                r.items().stream().map(ItemRequest::toLine).toList());
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Xác nhận phiếu: ghi sổ cái IN, tăng on_hand, tính lại giá vốn bình quân")
    public ReceiptView confirm(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return receiptService.confirm(user.tenantId(), user.userId(), id);
    }
}
