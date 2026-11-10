package com.oism.application.report;

import com.oism.domain.model.Branch;
import com.oism.domain.model.DailyReport;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.DailyReportRepository;
import com.oism.domain.repository.SalesAnalyticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chot so ngay (FR-SIM-03): tong hop vao daily_report theo (chi nhanh, kenh) cho mot ngay UTC.
 * Chi tinh don Confirmed/Completed nhu bao cao loi nhuan (FR-REP-01): doanh thu = SL x don gia,
 * gia von = SL x cost_price da chup luc Confirmed (FR-COST-02). Chay lai cho cung ngay thi ghi de.
 */
@Service
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final SalesAnalyticsRepository salesAnalytics;
    private final BranchRepository branchRepository;

    public DailyReportService(DailyReportRepository dailyReportRepository,
                              SalesAnalyticsRepository salesAnalytics,
                              BranchRepository branchRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.salesAnalytics = salesAnalytics;
        this.branchRepository = branchRepository;
    }

    /** Chot (hoac chot lai) ngay date cho tenant; tra ve cac dong vua ghi. */
    @Transactional
    public List<DailyReportView> generateDailyReport(UUID tenantId, LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        // Tong hop trong CSDL theo (chi nhanh, kenh): chi don Confirmed/Completed (NFR-PERF-01, khong IN (...) lon)
        List<SalesAnalyticsRepository.BranchChannelSales> groups = salesAnalytics.byBranchAndChannel(tenantId,
                new SalesAnalyticsRepository.SalesFilter(start, end, null, null, null));

        dailyReportRepository.deleteByTenantIdAndReportDate(tenantId, date);
        List<DailyReport> saved = new ArrayList<>();
        for (SalesAnalyticsRepository.BranchChannelSales g : groups) {
            saved.add(dailyReportRepository.save(new DailyReport(tenantId, g.branchId(), date, g.channel(),
                    (int) g.orderCount(), g.revenue().setScale(2, RoundingMode.HALF_UP),
                    g.cogs().setScale(2, RoundingMode.HALF_UP))));
        }
        return views(tenantId, saved);
    }

    @Transactional(readOnly = true)
    public List<DailyReportView> getDailyReports(UUID tenantId) {
        return views(tenantId, dailyReportRepository.findByTenantIdOrderByReportDateDescBranchIdAscChannelAsc(tenantId));
    }

    private List<DailyReportView> views(UUID tenantId, List<DailyReport> reports) {
        Map<UUID, String> branchNames = branchRepository.findByTenantIdOrderByNameAsc(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
        return reports.stream()
                .sorted(Comparator.comparing(DailyReport::getReportDate).reversed()
                        .thenComparing(r -> branchNames.getOrDefault(r.getBranchId(), ""))
                        .thenComparing(DailyReport::getChannel))
                .map(r -> new DailyReportView(r.getId(), r.getReportDate(), r.getBranchId(),
                        branchNames.getOrDefault(r.getBranchId(), "N/A"), r.getChannel(), r.getOrderCount(),
                        r.getRevenue(), r.getCogs(), r.getGrossProfit(), r.getCreatedAt()))
                .toList();
    }
}
