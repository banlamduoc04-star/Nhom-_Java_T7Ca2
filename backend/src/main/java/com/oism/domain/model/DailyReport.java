package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bao cao doanh thu, gia von va loi nhuan chot theo ngay (FR-SIM-03, JC-31).
 */
@Entity
@Table(name = "daily_report")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class DailyReport extends TenantScopedEntity {

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "total_revenue", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;

    @Column(name = "total_cogs", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalCogs = BigDecimal.ZERO;

    @Column(name = "gross_profit", nullable = false, precision = 15, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "completed_orders", nullable = false)
    private int completedOrders;

    @Column(name = "cancelled_orders", nullable = false)
    private int cancelledOrders;

    protected DailyReport() {
    }

    public DailyReport(UUID tenantId, LocalDate reportDate, BigDecimal totalRevenue, BigDecimal totalCogs,
                       BigDecimal grossProfit, int totalOrders, int completedOrders, int cancelledOrders) {
        super(tenantId);
        this.reportDate = reportDate;
        this.totalRevenue = totalRevenue != null ? totalRevenue : BigDecimal.ZERO;
        this.totalCogs = totalCogs != null ? totalCogs : BigDecimal.ZERO;
        this.grossProfit = grossProfit != null ? grossProfit : BigDecimal.ZERO;
        this.totalOrders = totalOrders;
        this.completedOrders = completedOrders;
        this.cancelledOrders = cancelledOrders;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public BigDecimal getTotalCogs() {
        return totalCogs;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public int getCompletedOrders() {
        return completedOrders;
    }

    public int getCancelledOrders() {
        return cancelledOrders;
    }
}
