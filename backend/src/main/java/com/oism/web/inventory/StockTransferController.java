package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.common.PageResult;
import com.oism.application.inventory.StockTransferService;
import com.oism.application.inventory.StockTransferService.TransferLine;
import com.oism.application.inventory.StockTransferService.TransferView;
import com.oism.domain.model.TransferStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-INV-03: Chuyen kho 2 buoc giua 2 chi nhanh. Owner, Staff. */
@RestController
@RequestMapping("/api/v1/transfers")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Stock Transfers")
public class StockTransferController {

    private final StockTransferService transferService;

    public StockTransferController(StockTransferService transferService) {
        this.transferService = transferService;
    }

    public record TransferItemRequest(
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 1, message = "Số lượng phải lớn hơn 0") int quantity) {
    }

    public record CreateTransferRequest(
            @NotNull(message = "Chi nhánh xuất không được để trống") UUID fromBranchId,
            @NotNull(message = "Chi nhánh nhận không được để trống") UUID toBranchId,
            @NotEmpty(message = "Phiếu chuyển cần ít nhất 1 dòng hàng") @Valid List<TransferItemRequest> items) {
    }

    @PostMapping
    @Operation(summary = "Tạo phiếu chuyển kho ở trạng thái Nháp (DRAFT)")
    public ResponseEntity<TransferView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                               @Valid @RequestBody CreateTransferRequest req) {
        List<TransferLine> lines = req.items().stream()
                .map(i -> new TransferLine(i.skuId(), i.quantity()))
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(
                transferService.create(user.tenantId(), user.userId(), req.fromBranchId(), req.toBranchId(), lines));
    }

    @PostMapping("/{id}/dispatch")
    @Operation(summary = "Xuất kho phiếu chuyển (DRAFT -> IN_TRANSIT); ghi sổ cái OUT tại kho xuất")
    public TransferView dispatch(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transferService.dispatch(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Nhận kho phiếu chuyển (IN_TRANSIT -> RECEIVED); ghi sổ cái IN tại kho nhận")
    public TransferView receive(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transferService.receive(user.tenantId(), user.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Hủy phiếu chuyển kho (chỉ áp dụng khi còn DRAFT)")
    public TransferView cancel(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transferService.cancel(user.tenantId(), user.userId(), id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiếu chuyển kho kèm danh sách dòng hàng")
    public TransferView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return transferService.get(user.tenantId(), id);
    }

    @GetMapping
    @Operation(summary = "Danh sách phiếu chuyển kho, lọc theo trạng thái/chi nhánh, phân trang")
    public PageResult<TransferView> list(@AuthenticationPrincipal AuthenticatedUser user,
                                         @RequestParam(required = false) TransferStatus status,
                                         @RequestParam(required = false) UUID fromBranchId,
                                         @RequestParam(required = false) UUID toBranchId,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return transferService.list(user.tenantId(), status, fromBranchId, toBranchId, page, size);
    }
}
