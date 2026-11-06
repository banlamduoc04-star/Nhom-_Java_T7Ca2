package com.oism.domain.dto;

import java.math.BigDecimal;

public class DashboardSummaryDTO {

    private BigDecimal totalRevenue;
    private BigDecimal grossProfit;
    private long lowStockCount;

    public DashboardSummaryDTO() {
    }

    // Constructor đầy đủ tham số
    public DashboardSummaryDTO(BigDecimal totalRevenue, BigDecimal grossProfit, long lowStockCount) {
        this.totalRevenue = totalRevenue;
        this.grossProfit = grossProfit;
        this.lowStockCount = lowStockCount;
    }

    // Getters & Setters
    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getGrossProfit() {
        return grossProfit;
    }

    public void setGrossProfit(BigDecimal grossProfit) {
        this.grossProfit = grossProfit;
    }

    public long getLowStockCount() {
        return lowStockCount;
    }

    public void setLowStockCount(long lowStockCount) {
        this.lowStockCount = lowStockCount;
    }
}