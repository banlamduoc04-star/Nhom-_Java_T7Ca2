package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.common.PageResult;
import com.oism.application.inventory.StocktakeService;
import com.oism.application.inventory.StocktakeService.CountInput;
import com.oism.application.inventory.StocktakeService.StocktakeView;
import com.oism.domain.model.StocktakeStatus;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** FR-INV-04: Kiem ke kho. Owner, Staff (Duyet: Owner). */
@RestController
@RequestMapping("/api/v1/stocktakes")
@PreAuthorize("hasAnyRole('OWNER','STAFF')")
@Tag(name = "Stocktakes")
public class StocktakeController {

    private final StocktakeService stocktakeService;

    public StocktakeController(StocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    public record CreateStocktakeRequest(
            @NotNull(message = "Chi nhánh kiểm kê không được để trống") UUID branchId,
            List<UUID> skuIds) {
    }

    public record CountItemRequest(
            @NotNull(message = "skuId không được để trống") UUID skuId,
            @Min(value = 0, message = "Số lượng đếm không được âm") int countedQuantity) {
    }

    public record RecordCountsRequest(
            @NotEmpty(message = "Cần ít nhất 1 dòng kết quả đếm") @Valid List<CountItemRequest> counts) {
    }

    @PostMapping
    @Operation(summary = "Mở phiên kiểm kê kho (OPEN); chụp tồn hệ thống system_qty")
    public ResponseEntity<StocktakeView> create(@AuthenticationPrincipal AuthenticatedUser user,
                                                @Valid @RequestBody CreateStocktakeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                stocktakeService.create(user.tenantId(), user.userId(), req.branchId(), req.skuIds()));
    }

    @PutMapping("/{id}/counts")
    @Operation(summary = "Nhập số lượng đếm thực tế cho phiên kiểm kê (tính difference = counted - system)")
    public StocktakeView recordCounts(@AuthenticationPrincipal AuthenticatedUser user,
                                      @PathVariable UUID id,
                                      @Valid @RequestBody RecordCountsRequest req) {
        List<CountInput> counts = req.counts().stream()
                .map(c -> new CountInput(c.skuId(), c.countedQuantity()))
                .toList();
        return stocktakeService.recordCounts(user.tenantId(), user.userId(), id, counts);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('OWNER')")
    @Operation(summary = "Duyệt phiên kiểm kê (OPEN -> APPROVED); tự động tạo bút toán điều chỉnh ADJUST (chỉ Owner)")
    public StocktakeView approve(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return stocktakeService.approve(user.tenantId(), user.userId(), id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết phiên kiểm kê kèm số lượng hệ thống, thực tế, chênh lệch")
    public StocktakeView get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return stocktakeService.get(user.tenantId(), id);
    }

    @GetMapping
    @Operation(summary = "Danh sách phiên kiểm kê, lọc theo trạng thái/chi nhánh, phân trang")
    public PageResult<StocktakeView> list(@AuthenticationPrincipal AuthenticatedUser user,
                                         @RequestParam(required = false) StocktakeStatus status,
                                         @RequestParam(required = false) UUID branchId,
                                         @RequestParam(required = false) Integer page,
                                         @RequestParam(required = false) Integer size) {
        return stocktakeService.list(user.tenantId(), status, branchId, page, size);
    }
}
