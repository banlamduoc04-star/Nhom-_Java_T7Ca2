package com.oism.domain.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryDTO(
    BigDecimal totalRevenue,
    BigDecimal grossProfit,
    long totalOrders,
    long lowStockCount,
    List<RevenueProfitPoint> chartData,
    List<LowStockItemDTO> lowStockItems
) {
    public record RevenueProfitPoint(String date, BigDecimal revenue, BigDecimal profit) {}
    public record LowStockItemDTO(String id, String skuCode, String productName, int availableStock, int threshold) {}
}