package com.oism.web.report;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.report.DailyReportService;
import com.oism.application.report.StockReportService;
import com.oism.application.report.StockReportViews;
import com.oism.domain.model.DailyReport;
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
import java.util.List;
import java.util.UUID;

@Tag(name = "Báo cáo tồn kho & hàng bán", description = "FR-REP-02, JC-29: Báo cáo giá trị tồn kho, top bán chạy, hàng tồn chậm, chốt số hàng ngày")
@RestController
@RequestMapping("/api/v1/reports")
public class StockReportController {

    private final StockReportService stockReportService;
    private final DailyReportService dailyReportService;

    public StockReportController(StockReportService stockReportService, DailyReportService dailyReportService) {
        this.stockReportService = stockReportService;
        this.dailyReportService = dailyReportService;
    }

    @Operation(summary = "Báo cáo định giá tồn kho theo chi nhánh / toàn tenant (FR-REP-02)")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/stock/valuation")
    public StockReportViews.ValuationSummary valuation(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "ID chi nhánh (để trống nếu xem toàn bộ cửa hàng)")
            @RequestParam(required = false) UUID branchId) {
        return stockReportService.getValuation(user.tenantId(), branchId);
    }

    @Operation(summary = "Top sản phẩm bán chạy theo số lượng và doanh thu (FR-REP-02)")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/stock/top-selling")
    public List<StockReportViews.TopSellingItem> topSelling(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Số ngày gần nhất tính doanh số", example = "30")
            @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Số lượng top SKU", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        return stockReportService.getTopSelling(user.tenantId(), days, limit);
    }

    @Operation(summary = "Danh sách hàng tồn chậm (còn tồn nhưng không bán trong N ngày) (FR-REP-02)")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/stock/slow-moving")
    public List<StockReportViews.SlowMovingItem> slowMoving(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Số ngày không phát sinh đơn hàng", example = "30")
            @RequestParam(defaultValue = "30") int days) {
        return stockReportService.getSlowMoving(user.tenantId(), days);
    }

    @Operation(summary = "Lịch sử báo cáo chốt sổ hàng ngày (FR-SIM-03, JC-31)")
    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/daily")
    public List<DailyReport> dailyReports(@AuthenticationPrincipal AuthenticatedUser user) {
        return dailyReportService.getDailyReports(user.tenantId());
    }

    @Operation(summary = "Chủ động chạy chốt sổ báo cáo ngày (Owner)")
    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/daily/run")
    public DailyReport triggerDailyReport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "Ngày cần chốt (mặc định hôm qua)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now().minusDays(1);
        return dailyReportService.generateDailyReport(user.tenantId(), targetDate);
    }
}
