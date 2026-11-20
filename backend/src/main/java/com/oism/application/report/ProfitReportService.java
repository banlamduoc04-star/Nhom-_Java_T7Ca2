package com.oism.application.report;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesAnalyticsRepository;
import com.oism.domain.repository.SalesAnalyticsRepository.SalesFilter;
import com.oism.domain.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bao cao doanh thu va loi nhuan gop (FR-REP-01): doanh thu thuan, tong gia von (tu order_item.cost_price),
 * loi nhuan gop = doanh thu - gia von. Chi tinh don Confirmed/Completed; loc ngay, chi nhanh, kenh, SKU; xuat CSV.
 * So lieu tong hop trong CSDL (SalesAnalyticsRepository) nen chay duoc voi 100.000 don (NFR-PERF-01).
 */
@Service
public class ProfitReportService {

    private final SalesAnalyticsRepository salesAnalytics;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final BranchRepository branchRepository;

    public ProfitReportService(SalesAnalyticsRepository salesAnalytics,
                               SkuRepository skuRepository,
                               ProductRepository productRepository,
                               BranchRepository branchRepository) {
        this.salesAnalytics = salesAnalytics;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.branchRepository = branchRepository;
    }

    /** grossMargin la ty le (0.3333 = 33,33%), lam tron 4 chu so. */
    public record ProfitSummary(BigDecimal totalRevenue, BigDecimal totalCogs, BigDecimal grossProfit,
                                BigDecimal grossMargin, long totalOrders, long totalQuantity) {
    }

    public record ProfitBySku(UUID skuId, String skuCode, String productName, long quantitySold,
                              BigDecimal revenue, BigDecimal cogs, BigDecimal grossProfit, BigDecimal grossMargin) {
    }

    public record ProfitReportView(ProfitSummary summary, List<ProfitBySku> bySku) {
    }

    @Transactional(readOnly = true)
    public ProfitReportView getProfitReport(UUID tenantId, Instant from, Instant to, UUID branchId,
                                            OrderChannel channel, UUID skuId) {
        SalesFilter filter = filter(from, to, branchId, channel, skuId);
        SalesAnalyticsRepository.SalesTotals totals = salesAnalytics.totals(tenantId, filter);
        List<SalesAnalyticsRepository.SkuSales> bySku = salesAnalytics.bySku(tenantId, filter);

        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId,
                bySku.stream().map(SalesAnalyticsRepository.SkuSales::skuId).toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<UUID, String> productNames = productRepository.findByTenantIdAndIdIn(tenantId,
                skus.values().stream().map(Sku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<ProfitBySku> rows = bySku.stream().map(s -> {
            Sku sku = skus.get(s.skuId());
            String code = sku != null ? sku.getCode() : s.skuId().toString();
            String name = sku != null ? productNames.getOrDefault(sku.getProductId(), code) : code;
            BigDecimal revenue = money(s.revenue());
            BigDecimal cogs = money(s.cogs());
            return new ProfitBySku(s.skuId(), code, name, s.quantity(), revenue, cogs, revenue.subtract(cogs),
                    margin(revenue, cogs));
        }).toList();

        BigDecimal revenue = money(totals.revenue());
        BigDecimal cogs = money(totals.cogs());
        return new ProfitReportView(new ProfitSummary(revenue, cogs, revenue.subtract(cogs), margin(revenue, cogs),
                totals.orderCount(), totals.quantity()), rows);
    }

    /** CSV chi tiet tung dong hang (FR-REP-01 "xuat CSV"); so dung dau cham thap phan, khong phu thuoc locale. */
    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId, Instant from, Instant to, UUID branchId, OrderChannel channel, UUID skuId) {
        SalesFilter filter = filter(from, to, branchId, channel, skuId);
        List<SalesAnalyticsRepository.SalesLine> lines = salesAnalytics.lines(tenantId, filter);
        Map<UUID, String> branchNames = branchRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName, (a, b) -> a));
        Map<UUID, String> skuCodes = skuRepository.findByTenantIdAndIdIn(tenantId,
                lines.stream().map(SalesAnalyticsRepository.SalesLine::skuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));

        StringBuilder sb = new StringBuilder("﻿");
        sb.append("Mã đơn,Ngày,Chi nhánh,Kênh,Mã SKU,Số lượng,Đơn giá,Giá vốn,Doanh thu,Tổng giá vốn,Lợi nhuận gộp,Tỷ suất LN (%)\n");
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCogs = BigDecimal.ZERO;
        long totalQty = 0;
        for (SalesAnalyticsRepository.SalesLine l : lines) {
            BigDecimal qty = BigDecimal.valueOf(l.quantity());
            BigDecimal cost = l.costPrice() != null ? l.costPrice() : BigDecimal.ZERO;
            BigDecimal revenue = money(l.unitPrice().multiply(qty));
            BigDecimal cogs = money(cost.multiply(qty));
            totalRevenue = totalRevenue.add(revenue);
            totalCogs = totalCogs.add(cogs);
            totalQty += l.quantity();
            sb.append(csv(l.orderCode())).append(',').append(l.orderDate()).append(',')
                    .append(csv(branchNames.getOrDefault(l.branchId(), ""))).append(',').append(l.channel()).append(',')
                    .append(csv(skuCodes.getOrDefault(l.skuId(), l.skuId().toString()))).append(',')
                    .append(l.quantity()).append(',').append(money(l.unitPrice()).toPlainString()).append(',')
                    .append(money(cost).toPlainString()).append(',').append(revenue.toPlainString()).append(',')
                    .append(cogs.toPlainString()).append(',').append(revenue.subtract(cogs).toPlainString()).append(',')
                    .append(percent(revenue, cogs)).append('\n');
        }
        sb.append("\"TỔNG CỘNG\",,,,,").append(totalQty).append(",,,").append(totalRevenue.toPlainString()).append(',')
                .append(totalCogs.toPlainString()).append(',').append(totalRevenue.subtract(totalCogs).toPlainString())
                .append(',').append(percent(totalRevenue, totalCogs)).append('\n');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static SalesFilter filter(Instant from, Instant to, UUID branchId, OrderChannel channel, UUID skuId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.RP40001, "Khoảng thời gian không hợp lệ: 'from' phải trước hoặc bằng 'to'");
        }
        return new SalesFilter(from, to, branchId, channel, skuId);
    }

    private static BigDecimal money(BigDecimal v) {
        return (v != null ? v : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal margin(BigDecimal revenue, BigDecimal cogs) {
        return revenue.signum() > 0
                ? revenue.subtract(cogs).divide(revenue, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(4);
    }

    private static String percent(BigDecimal revenue, BigDecimal cogs) {
        return margin(revenue, cogs).movePointRight(2).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
