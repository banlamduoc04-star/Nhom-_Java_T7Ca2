package com.oism.infrastructure.repository;

import org.springframework.stereotype.Repository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;

@Repository
public class ReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public BigDecimal calculateTotalRevenue() {
        Object result = entityManager.createNativeQuery(
            "SELECT COALESCE(SUM(total_amount), 0) FROM sales_order WHERE status IN ('CONFIRMED', 'COMPLETED')"
        ).getSingleResult();
        return new BigDecimal(result.toString());
    }

    public BigDecimal calculateGrossProfit() {
        Object result = entityManager.createNativeQuery(
            "SELECT COALESCE(SUM((oi.unit_price - oi.cost_price) * oi.quantity), 0) " +
            "FROM sales_order so JOIN order_item oi ON so.id = oi.order_id " +
            "WHERE so.status IN ('CONFIRMED', 'COMPLETED')"
        ).getSingleResult();
        return new BigDecimal(result.toString());
    }

    public long countLowStockItems() {
        Object result = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM stock_balance WHERE available_quantity <= min_threshold"
        ).getSingleResult();
        return ((Number) result).longValue();
    }
}