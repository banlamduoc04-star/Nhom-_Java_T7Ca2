package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.common.PageResult;
import com.oism.application.inventory.StockService;
import com.oism.application.inventory.StockView;
import com.oism.application.inventory.TransactionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** FR-INV-01, FR-RSE-01: xem ton kho va so cai. Khong co API sua/xoa so cai (NFR-SEC-03). */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Inventory")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/stock")
    @Operation(summary = "Xem on_hand, reserved, available theo chi nhánh/SKU")
    public PageResult<StockView> stock(@AuthenticationPrincipal AuthenticatedUser user,
                                        @RequestParam(required = false) UUID branchId,
                                        @RequestParam(required = false) UUID skuId,
                                        @RequestParam(required = false) Integer page,
                                        @RequestParam(required = false) Integer size) {
        return stockService.stock(user.tenantId(), branchId, skuId, page, size);
    }

    @GetMapping("/inventory-transactions")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @Operation(summary = "Xem sổ cái tồn kho (Owner, Staff); from/to dạng ISO-8601, to không bao gồm")
    public PageResult<TransactionView> transactions(@AuthenticationPrincipal AuthenticatedUser user,
                                                    @RequestParam(required = false) UUID branchId,
                                                    @RequestParam(required = false) UUID skuId,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                    @RequestParam(required = false)
                                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return stockService.transactions(user.tenantId(), branchId, skuId, from, to, page, size);
    }

    @GetMapping("/inventory-transactions/keyset")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @Operation(summary = "Xem sổ cái tồn kho phân trang keyset (JC-30, FR-INV-01)")
    public List<TransactionView> transactionsKeyset(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastCreatedAt,
            @RequestParam(required = false) UUID lastId,
            @RequestParam(defaultValue = "20") int limit) {
        return stockService.transactionsKeyset(user.tenantId(), branchId, lastCreatedAt, lastId, limit);
    }
}
