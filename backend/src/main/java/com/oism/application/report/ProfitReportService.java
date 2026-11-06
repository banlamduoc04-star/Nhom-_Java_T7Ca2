package com.oism.application.report;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bao cao loi nhuan gop (FR-REP-01):
 * Tinh tu order_item.cost_price da chup khi Confirmed (FR-COST-02).
 * Doanh thu = SL x Gia ban; Gia von = SL x Cost price; Loi nhuan = Doanh thu - Gia von; Bien LN = (Loi nhuan / Doanh thu) x 100%.
 */
@Service
public class ProfitReportService {

    private static final Set<OrderStatus> PROFIT_STATUSES = Set.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED);

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final SkuRepository skuRepository;
    private final BranchRepository branchRepository;

    public ProfitReportService(SalesOrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               SkuRepository skuRepository,
                               BranchRepository branchRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.skuRepository = skuRepository;
        this.branchRepository = branchRepository;
    }

    public record ProfitRow(
            String orderCode,
            UUID branchId,
            String branchName,
            OrderChannel channel,
            UUID skuId,
            String skuCode,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal costPrice,
            BigDecimal revenue,
            BigDecimal totalCost,
            BigDecimal grossProfit,
            BigDecimal marginPercent,
            Instant orderDate) {
    }

    public record ProfitSummary(
            BigDecimal totalRevenue,
            BigDecimal totalCost,
            BigDecimal totalGrossProfit,
            BigDecimal grossMarginPercent,
            int totalOrders,
            int totalQuantity,
            List<ProfitRow> rows) {
    }

    @Transactional(readOnly = true)
    public ProfitSummary getProfitReport(UUID tenantId, Instant from, Instant to, UUID branchId, OrderChannel channel, UUID skuId) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.RP40001, "Khoảng thời gian không hợp lệ: 'from' phải trước hoặc bằng 'to'");
        }
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            ps.add(root.get("status").in(PROFIT_STATUSES));
            if (branchId != null) {
                ps.add(cb.equal(root.get("branchId"), branchId));
            }
            if (channel != null) {
                ps.add(cb.equal(root.get("channel"), channel));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        List<SalesOrder> orders = orderRepository.findAll(spec);
        if (orders.isEmpty()) {
            return new ProfitSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, List.of());
        }

        Map<UUID, SalesOrder> orderMap = orders.stream().collect(Collectors.toMap(SalesOrder::getId, o -> o));
        List<UUID> orderIds = new ArrayList<>(orderMap.keySet());

        List<OrderItem> items = orderItemRepository.findByTenantIdAndOrderIdIn(tenantId, orderIds);
        if (skuId != null) {
            items = items.stream().filter(i -> skuId.equals(i.getSkuId())).toList();
        }

        Map<UUID, String> skuCodes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(OrderItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));

        Map<UUID, String> branchNames = branchRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(Branch::getId, Branch::getName));

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        int totalQty = 0;
        List<ProfitRow> rows = new ArrayList<>();

        for (OrderItem item : items) {
            SalesOrder o = orderMap.get(item.getOrderId());
            if (o == null) continue;

            BigDecimal unitPrice = item.getUnitPrice();
            BigDecimal costPrice = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            int qty = item.getQuantity();

            BigDecimal revenue = unitPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal cost = costPrice.multiply(BigDecimal.valueOf(qty));
            BigDecimal profit = revenue.subtract(cost);
            BigDecimal margin = revenue.compareTo(BigDecimal.ZERO) > 0
                    ? profit.multiply(BigDecimal.valueOf(100)).divide(revenue, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            totalRevenue = totalRevenue.add(revenue);
            totalCost = totalCost.add(cost);
            totalQty += qty;

            rows.add(new ProfitRow(
                    o.getCode(),
                    o.getBranchId(),
                    branchNames.getOrDefault(o.getBranchId(), o.getBranchId().toString()),
                    o.getChannel(),
                    item.getSkuId(),
                    skuCodes.getOrDefault(item.getSkuId(), item.getSkuId().toString()),
                    qty,
                    unitPrice,
                    costPrice,
                    revenue,
                    cost,
                    profit,
                    margin,
                    o.getCreatedAt()
            ));
        }

        BigDecimal totalGrossProfit = totalRevenue.subtract(totalCost);
        BigDecimal totalMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalGrossProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int totalOrdersCount = (int) rows.stream().map(ProfitRow::orderCode).distinct().count();

        return new ProfitSummary(totalRevenue, totalCost, totalGrossProfit, totalMargin, totalOrdersCount, totalQty, rows);
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv(UUID tenantId, Instant from, Instant to, UUID branchId, OrderChannel channel, UUID skuId) {
        ProfitSummary summary = getProfitReport(tenantId, from, to, branchId, channel, skuId);
        StringBuilder sb = new StringBuilder();
        // BOM for Excel UTF-8 display
        sb.append('\ufeff');
        sb.append("Mã đơn,Chi nhánh,Kênh,Mã SKU,Số lượng,Đơn giá,Giá vốn,Doanh thu,Tổng giá vốn,Lợi nhuận gộp,Tỷ suất LN (%)\n");

        for (ProfitRow r : summary.rows()) {
            sb.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\",%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f\n",
                    r.orderCode(), r.branchName(), r.channel(), r.skuCode(), r.quantity(),
                    r.unitPrice(), r.costPrice(), r.revenue(), r.totalCost(), r.grossProfit(), r.marginPercent()));
        }

        sb.append(String.format("\n\"TỔNG CỘNG\",\"\",\"\",\"\",%d,\"\",\"\",%.2f,%.2f,%.2f,%.2f\n",
                summary.totalQuantity(), summary.totalRevenue(), summary.totalCost(), summary.totalGrossProfit(), summary.grossMarginPercent()));

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
