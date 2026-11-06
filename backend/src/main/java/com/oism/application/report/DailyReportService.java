package com.oism.application.report;

import com.oism.domain.model.DailyReport;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.repository.DailyReportRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tong hop bao cao doanh thu, gia von va loi nhuan theo ngay (FR-SIM-03, JC-31).
 */
@Service
public class DailyReportService {

    private final DailyReportRepository dailyReportRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderItemRepository orderItemRepository;

    public DailyReportService(DailyReportRepository dailyReportRepository,
                              SalesOrderRepository salesOrderRepository,
                              OrderItemRepository orderItemRepository) {
        this.dailyReportRepository = dailyReportRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public DailyReport generateDailyReport(UUID tenantId, LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = date.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        List<SalesOrder> orders = salesOrderRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("tenantId"), tenantId),
                cb.greaterThanOrEqualTo(root.get("createdAt"), start),
                cb.lessThan(root.get("createdAt"), end)
        ));

        int totalOrders = orders.size();
        int completedOrders = 0;
        int cancelledOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;

        List<UUID> completedOrderIds = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED || o.getStatus() == OrderStatus.COMPLETED)
                .map(SalesOrder::getId)
                .toList();

        for (SalesOrder o : orders) {
            if (o.getStatus() == OrderStatus.COMPLETED) {
                completedOrders++;
            } else if (o.getStatus() == OrderStatus.CANCELLED) {
                cancelledOrders++;
            }
        }

        if (!completedOrderIds.isEmpty()) {
            List<OrderItem> items = orderItemRepository.findByTenantIdAndOrderIdIn(tenantId, completedOrderIds);

            for (OrderItem it : items) {
                BigDecimal qty = BigDecimal.valueOf(it.getQuantity());
                totalRevenue = totalRevenue.add(it.getUnitPrice().multiply(qty));
                if (it.getCostPrice() != null) {
                    totalCogs = totalCogs.add(it.getCostPrice().multiply(qty));
                }
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalCogs);

        Optional<DailyReport> existing = dailyReportRepository.findByTenantIdAndReportDate(tenantId, date);
        if (existing.isPresent()) {
            dailyReportRepository.delete(existing.get());
        }

        DailyReport report = new DailyReport(
                tenantId, date, totalRevenue, totalCogs, grossProfit,
                totalOrders, completedOrders, cancelledOrders
        );
        return dailyReportRepository.save(report);
    }

    @Transactional(readOnly = true)
    public List<DailyReport> getDailyReports(UUID tenantId) {
        return dailyReportRepository.findByTenantIdOrderByReportDateDesc(tenantId);
    }
}
