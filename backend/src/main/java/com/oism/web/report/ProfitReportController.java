package com.oism.web.report;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.application.report.ProfitReportService;
import com.oism.application.report.ProfitReportService.ProfitReportView;
import com.oism.domain.model.OrderChannel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

/** FR-REP-01: Bao cao doanh thu va loi nhuan gop. Chi Owner co quyen xem (NFR-SEC). */
@RestController
@RequestMapping("/api/v1/reports/profit")
@PreAuthorize("hasRole('OWNER')")
@Tag(name = "Reports")
public class ProfitReportController {

    private final ProfitReportService profitReportService;

    public ProfitReportController(ProfitReportService profitReportService) {
        this.profitReportService = profitReportService;
    }

    @GetMapping
    @Operation(summary = "Doanh thu, giá vốn, lợi nhuận gộp (tổng + theo SKU), đơn Confirmed/Completed; lọc ngày, chi nhánh, kênh, SKU - Owner. Lỗi: 400 RP40001")
    public ProfitReportView getProfitReport(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) UUID skuId) {
        return profitReportService.getProfitReport(user.tenantId(), from, to, branchId, channel, skuId);
    }

    @GetMapping("/export")
    @Operation(summary = "Xuất báo cáo lợi nhuận gộp ra định dạng file CSV")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) OrderChannel channel,
            @RequestParam(required = false) UUID skuId) {
        byte[] csv = profitReportService.exportCsv(user.tenantId(), from, to, branchId, channel, skuId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bao-cao-loi-nhuan.csv")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }
}
