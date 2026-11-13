package com.oism.domain.repository;

import com.oism.domain.model.OrderChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Truy van tong hop doanh so tinh ngay trong CSDL (SUM/GROUP BY) cho bao cao (FR-REP-01/02, FR-SIM-03).
 * Chi tinh don CONFIRMED/COMPLETED; gia von lay tu order_item.cost_price da chup (FR-COST-02).
 * Thay cho cach nap toan bo don vao bo nho roi IN (...) - cach do vuot gioi han 65.535 tham so cua PostgreSQL
 * khi co tren 65 nghin don (NFR-PERF-01 yeu cau bao cao chay voi 100.000 ban ghi).
 */
public interface SalesAnalyticsRepository {

    /** Bo loc tuy chon; null = khong loc. Khoang thoi gian [from, to) theo orders.created_at. */
    record SalesFilter(Instant from, Instant to, UUID branchId, OrderChannel channel, UUID skuId) {

        public static SalesFilter since(Instant from) {
            return new SalesFilter(from, null, null, null, null);
        }
    }

    record SalesTotals(long orderCount, long quantity, BigDecimal revenue, BigDecimal cogs) {
    }

    record SkuSales(UUID skuId, long quantity, BigDecimal revenue, BigDecimal cogs) {
    }

    record BranchChannelSales(UUID branchId, OrderChannel channel, long orderCount, BigDecimal revenue, BigDecimal cogs) {
    }

    record SalesLine(String orderCode, UUID branchId, OrderChannel channel, UUID skuId, int quantity,
                     BigDecimal unitPrice, BigDecimal costPrice, Instant orderDate) {
    }

    SalesTotals totals(UUID tenantId, SalesFilter filter);

    /** Theo SKU, xep doanh thu giam dan. */
    List<SkuSales> bySku(UUID tenantId, SalesFilter filter);

    /** Theo SKU, xep so luong ban giam dan, toi da limit dong (top ban chay). */
    List<SkuSales> topSkusByQuantity(UUID tenantId, SalesFilter filter, int limit);

    /** Cac SKU co phat sinh ban trong khoang loc. */
    List<UUID> soldSkuIds(UUID tenantId, SalesFilter filter);

    List<BranchChannelSales> byBranchAndChannel(UUID tenantId, SalesFilter filter);

    /** Chi tiet tung dong hang (xuat CSV), xep theo thoi gian don. */
    List<SalesLine> lines(UUID tenantId, SalesFilter filter);
}
