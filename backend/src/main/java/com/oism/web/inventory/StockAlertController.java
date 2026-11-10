package com.oism.web.inventory;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.inventory.StockAlertService;
import com.oism.application.inventory.StockAlertView;
import com.oism.domain.model.StockAlert;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Cảnh báo tồn kho", description = "FR-REP-03, JC-30: Quản lý cảnh báo tồn kho thấp dưới ngưỡng đặt hàng lại")
@RestController
@RequestMapping("/api/v1/alerts/stock")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @Operation(summary = "Danh sách cảnh báo tồn kho thấp")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @GetMapping
    public List<StockAlertView> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) StockAlert.Status status,
            @RequestParam(required = false) UUID branchId) {
        return stockAlertService.listAlerts(user.tenantId(), status, branchId);
    }

    @Operation(summary = "Đóng cảnh báo tồn kho thủ công")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @PostMapping("/{id}/resolve")
    public Map<String, String> resolve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        stockAlertService.closeAlert(user.tenantId(), id);
        return Map.of("message", "Đã đóng cảnh báo thành công");
    }

    @Operation(summary = "Số lượng cảnh báo đang mở (cho badge Dashboard)")
    @PreAuthorize("hasAnyRole('OWNER', 'STAFF')")
    @GetMapping("/count")
    public Map<String, Long> countOpen(@AuthenticationPrincipal AuthenticatedUser user) {
        return Map.of("openAlerts", stockAlertService.countOpenAlerts(user.tenantId()));
    }
}

