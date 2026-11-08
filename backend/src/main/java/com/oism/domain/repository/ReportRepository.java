package com.oism.domain.repository;

import java.math.BigDecimal;

public interface ReportRepository {

    BigDecimal calculateTotalRevenue();

    BigDecimal calculateGrossProfit();

    long countLowStockItems();
}
