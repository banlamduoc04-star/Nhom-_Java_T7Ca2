package com.oism.web.report;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.report.DailyReportService;
import com.oism.application.report.DailyReportView;
import com.oism.application.report.StockReportService;
import com.oism.application.report.StockReportViews;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Bao cao ton kho va toc do ban (FR-REP-02) theo Danh sach API cua ke hoach:
 * GET /reports/inventory-value, /reports/top-selling, /reports/slow-moving - Owner, Staff.
 * (bo sung) Bao cao chot ngay FR-SIM-03: GET /reports/daily, POST /reports/daily/run - Owner.
 */
@Tag(name = "Báo cáo tồn kho & hàng bán", description = "FR-REP-02: giá trị tồn, top bán chạy, hàng tồn chậm; FR-SIM-03: chốt sổ ngày")
@RestController
@RequestMapping("/api/v1/reports")
public class StockReportController {

    private final StockReportService stockReportService;
    private final DailyReportService dailyReportService;

    public StockReportController(StockReportService stockReportService, DailyReportService dailyReportService) {
        this.stockReportService = stockReportService;
        this.dailyReportService = dailyReportService;
    }

    @Operation(summary = "Giá trị tồn = on_hand × giá vốn, theo chi nhánh hoặc toàn cửa hàng (FR-REP-02) - Owner, Staff")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @GetMapping("/inventory-value")
    public StockReportViews.ValuationSummary inventoryValue(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "ID chi nhánh (để trống nếu xem toàn bộ cửa hàng)")
            @RequestParam(required = false) UUID branchId) {
        return stockReportService.getValuation(user.tenantId(), branchId);
    }

    @Operation(summary = "Top bán chạy theo số lượng và doanh thu (mặc định top 10, 30 ngày) (FR-REP-02) - Owner, Staff")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @GetMapping("/top-selling")
    public List<StockReportViews.TopSellingItem> topSelling(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Số ngày gần nhất tính doanh số", example = "30")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Số lượng top SKU", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return stockReportService.getTopSelling(user.tenantId(), days, limit);
    }

    @Operation(summary = "Hàng tồn chậm: còn tồn nhưng không bán trong N ngày (mặc định 30) (FR-REP-02) - Owner, Staff")
    @PreAuthorize("hasAnyRole('OWNER','STAFF')")
    @GetMapping("/slow-moving")
    public List<StockReportViews.SlowMovingItem> slowMoving(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Số ngày không phát sinh đơn hàng", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return stockReportService.getSlowMoving(user.tenantId(), days);
    }

    @Operation(summary = "(bổ sung) Lịch sử chốt sổ ngày theo chi nhánh × kênh (FR-SIM-03) - Owner")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/daily")
    public List<DailyReportView> dailyReports(@AuthenticationPrincipal AuthenticatedUser user) {
        return dailyReportService.getDailyReports(user.tenantId());
    }

    @Operation(summary = "(bổ sung) Chốt sổ một ngày ngay (mặc định hôm qua theo UTC), chạy lại thì ghi đè - Owner")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/daily/run")
    public List<DailyReportView> triggerDailyReport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Ngày cần chốt (yyyy-MM-dd), mặc định hôm qua")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(ZoneOffset.UTC).minusDays(1);
        return dailyReportService.generateDailyReport(user.tenantId(), targetDate);
    }
}
