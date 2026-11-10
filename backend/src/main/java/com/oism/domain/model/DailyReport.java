package com.oism.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Bao cao chot ngay (FR-SIM-03; Ke hoach muc 4 bang daily_report): moi dong = 1 chi nhanh x 1 ngay x 1 kenh.
 * Chi tinh don Confirmed/Completed (nhu FR-REP-01); gia von lay tu order_item.cost_price da chup.
 */
@Entity
@Table(name = "daily_report")
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = TenantScopedEntity.TENANT_FILTER_CONDITION)
public class DailyReport extends TenantScopedEntity {

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private OrderChannel channel;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "revenue", nullable = false, precision = 19, scale = 2)
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "cogs", nullable = false, precision = 19, scale = 2)
    private BigDecimal cogs = BigDecimal.ZERO;

    @Column(name = "gross_profit", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossProfit = BigDecimal.ZERO;

    protected DailyReport() {
    }

    public DailyReport(UUID tenantId, UUID branchId, LocalDate reportDate, OrderChannel channel,
                       int orderCount, BigDecimal revenue, BigDecimal cogs) {
        super(tenantId);
        this.branchId = branchId;
        this.reportDate = reportDate;
        this.channel = channel;
        this.orderCount = orderCount;
        this.revenue = revenue;
        this.cogs = cogs;
        this.grossProfit = revenue.subtract(cogs);
    }

    public UUID getBranchId() {
        return branchId;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public OrderChannel getChannel() {
        return channel;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public BigDecimal getCogs() {
        return cogs;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }
}
