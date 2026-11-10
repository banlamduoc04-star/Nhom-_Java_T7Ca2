package com.oism.application.report;

import com.oism.domain.model.Branch;
import com.oism.domain.model.DailyReport;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.DailyReportRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Chot so ngay (FR-SIM-03): tong hop vao daily_report theo (chi nhanh, kenh) cho mot ngay UTC.
 * Chi tinh don Confirmed/Completed nhu bao cao loi nhuan (FR-REP-01): doanh thu = SL x don gia,
 * gia von = SL x cost_price da chup luc Confirmed (FR-COST-02). Chay lai cho cung ngay thi ghi de.
 */
@Service
public class DailyReportService {

    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED);

    private final DailyReportRepository dailyReportRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BranchRepository branchRepository;

    public DailyReportService(DailyReportRepository dailyReportRepository,
                              SalesOrderRepository salesOrderRepository,
                              OrderItemRepository orderItemRepository,
                              BranchRepository branchRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.branchRepository = branchRepository;
    }

    /** Chot (hoac chot lai) ngay date cho tenant; tra ve cac dong vua ghi. */
    @Transactional
    public List<DailyReportView> generateDailyReport(UUID tenantId, LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<SalesOrder> orders = salesOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                root.get("status").in(REVENUE_STATUSES),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)));

        Map<UUID, List<OrderItem>> itemsByOrder = orders.isEmpty() ? Map.of()
                : orderItemRepository.findByTenantIdAndOrderIdIn(tenantId, orders.stream().map(SalesOrder::getId).toList())
                .stream().collect(Collectors.groupingBy(OrderItem::getOrderId));

        // (chi nhanh, kenh) -> [so don, doanh thu, gia von]; TreeMap de thu tu ghi on dinh
        Map<String, Totals> groups = new TreeMap<>();
        for (SalesOrder o : orders) {
            Totals t = groups.computeIfAbsent(o.getBranchId() + "|" + o.getChannel(),
                    k -> new Totals(o.getBranchId(), o.getChannel()));
            t.orderCount++;
            for (OrderItem it : itemsByOrder.getOrDefault(o.getId(), List.of())) {
                BigDecimal qty = BigDecimal.valueOf(it.getQuantity());
                t.revenue = t.revenue.add(it.getUnitPrice().multiply(qty));
                if (it.getCostPrice() != null) {
                    t.cogs = t.cogs.add(it.getCostPrice().multiply(qty));
                }
            }
        }

        dailyReportRepository.deleteByTenantIdAndReportDate(tenantId, date);
        List<DailyReport> saved = new ArrayList<>();
        for (Totals t : groups.values()) {
            saved.add(dailyReportRepository.save(new DailyReport(tenantId, t.branchId, date, t.channel, t.orderCount,
                    t.revenue.setScale(2, RoundingMode.HALF_UP), t.cogs.setScale(2, RoundingMode.HALF_UP))));
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

    private static final class Totals {
        private final UUID branchId;
        private final OrderChannel channel;
        private int orderCount;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal cogs = BigDecimal.ZERO;

        private Totals(UUID branchId, OrderChannel channel) {
            this.branchId = branchId;
            this.channel = channel;
        }
    }
}
