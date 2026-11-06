package com.oism.infrastructure.repository;

import com.oism.domain.repository.ReportRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class JpaReportRepository implements ReportRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BigDecimal calculateTotalRevenue() {
        Object result = entityManager.createNativeQuery(
            "SELECT COALESCE(SUM(total_amount), 0) FROM sales_order WHERE status IN ('CONFIRMED', 'COMPLETED')"
        ).getSingleResult();
        return new BigDecimal(result.toString());
    }

    @Override
    public BigDecimal calculateGrossProfit() {
        Object result = entityManager.createNativeQuery(
            "SELECT COALESCE(SUM((oi.unit_price - oi.cost_price) * oi.quantity), 0) " +
            "FROM sales_order so JOIN order_item oi ON so.id = oi.order_id " +
            "WHERE so.status IN ('CONFIRMED', 'COMPLETED')"
        ).getSingleResult();
        return new BigDecimal(result.toString());
    }

    @Override
    public long countLowStockItems() {
        Object result = entityManager.createNativeQuery(
            "SELECT COUNT(*) FROM stock_balance WHERE available_quantity <= min_threshold"
        ).getSingleResult();
        return ((Number) result).longValue();
    }
}