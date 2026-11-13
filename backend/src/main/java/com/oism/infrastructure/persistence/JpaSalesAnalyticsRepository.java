package com.oism.infrastructure.persistence;

import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.repository.SalesAnalyticsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cai dat SalesAnalyticsRepository bang JPQL tong hop. Dieu kien loc duoc ghep dong theo bo loc
 * (khong dung "tham so IS NULL" vi PostgreSQL khong suy ra kieu cua tham so null).
 * Moi truy van deu loc tenant_id ca o orders va order_item (NFR-TENANT-01).
 */
@Repository
public class JpaSalesAnalyticsRepository implements SalesAnalyticsRepository {

    private static final Set<OrderStatus> REVENUE_STATUSES = Set.of(OrderStatus.CONFIRMED, OrderStatus.COMPLETED);
    private static final String FROM_JOIN = " from OrderItem i join SalesOrder o on o.id = i.orderId ";
    private static final String REVENUE = "sum(i.unitPrice * i.quantity)";
    private static final String COGS = "sum(coalesce(i.costPrice, 0) * i.quantity)";

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public SalesTotals totals(UUID tenantId, SalesFilter filter) {
        Map<String, Object> params = new HashMap<>();
        Object[] row = query("select count(distinct o.id), coalesce(sum(i.quantity), 0), coalesce(" + REVENUE
                + ", 0), coalesce(" + COGS + ", 0)" + FROM_JOIN + where(tenantId, filter, params), Object[].class, params)
                .getSingleResult();
        return new SalesTotals(((Number) row[0]).longValue(), ((Number) row[1]).longValue(),
                (BigDecimal) row[2], (BigDecimal) row[3]);
    }

    @Override
    public List<SkuSales> bySku(UUID tenantId, SalesFilter filter) {
        return skuQuery(tenantId, filter, REVENUE + " desc", 0);
    }

    @Override
    public List<SkuSales> topSkusByQuantity(UUID tenantId, SalesFilter filter, int limit) {
        return skuQuery(tenantId, filter, "sum(i.quantity) desc, " + REVENUE + " desc", limit);
    }

    @Override
    public List<UUID> soldSkuIds(UUID tenantId, SalesFilter filter) {
        Map<String, Object> params = new HashMap<>();
        return query("select distinct i.skuId" + FROM_JOIN + where(tenantId, filter, params), UUID.class, params)
                .getResultList();
    }

    @Override
    public List<BranchChannelSales> byBranchAndChannel(UUID tenantId, SalesFilter filter) {
        Map<String, Object> params = new HashMap<>();
        return query("select o.branchId, o.channel, count(distinct o.id), " + REVENUE + ", " + COGS + FROM_JOIN
                + where(tenantId, filter, params) + " group by o.branchId, o.channel", Object[].class, params)
                .getResultList().stream()
                .map(r -> new BranchChannelSales((UUID) r[0], (OrderChannel) r[1], ((Number) r[2]).longValue(),
                        (BigDecimal) r[3], (BigDecimal) r[4]))
                .toList();
    }

    @Override
    public List<SalesLine> lines(UUID tenantId, SalesFilter filter) {
        Map<String, Object> params = new HashMap<>();
        return query("select o.code, o.branchId, o.channel, i.skuId, i.quantity, i.unitPrice, i.costPrice, o.createdAt"
                + FROM_JOIN + where(tenantId, filter, params) + " order by o.createdAt, o.code", Object[].class, params)
                .getResultList().stream()
                .map(r -> new SalesLine((String) r[0], (UUID) r[1], (OrderChannel) r[2], (UUID) r[3],
                        ((Number) r[4]).intValue(), (BigDecimal) r[5], (BigDecimal) r[6], (Instant) r[7]))
                .toList();
    }

    private List<SkuSales> skuQuery(UUID tenantId, SalesFilter filter, String orderBy, int limit) {
        Map<String, Object> params = new HashMap<>();
        TypedQuery<Object[]> q = query("select i.skuId, sum(i.quantity), " + REVENUE + ", " + COGS + FROM_JOIN
                + where(tenantId, filter, params) + " group by i.skuId order by " + orderBy, Object[].class, params);
        if (limit > 0) {
            q.setMaxResults(limit);
        }
        return q.getResultList().stream()
                .map(r -> new SkuSales((UUID) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2], (BigDecimal) r[3]))
                .toList();
    }

    private static String where(UUID tenantId, SalesFilter f, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(" where o.tenantId = :tenantId and i.tenantId = :tenantId and o.status in :statuses");
        params.put("tenantId", tenantId);
        params.put("statuses", REVENUE_STATUSES);
        if (f != null) {
            if (f.from() != null) {
                sb.append(" and o.createdAt >= :from");
                params.put("from", f.from());
            }
            if (f.to() != null) {
                sb.append(" and o.createdAt < :to");
                params.put("to", f.to());
            }
            if (f.branchId() != null) {
                sb.append(" and o.branchId = :branchId");
                params.put("branchId", f.branchId());
            }
            if (f.channel() != null) {
                sb.append(" and o.channel = :channel");
                params.put("channel", f.channel());
            }
            if (f.skuId() != null) {
                sb.append(" and i.skuId = :skuId");
                params.put("skuId", f.skuId());
            }
        }
        return sb.toString();
    }

    private <T> TypedQuery<T> query(String jpql, Class<T> type, Map<String, Object> params) {
        TypedQuery<T> q = entityManager.createQuery(jpql, type);
        params.forEach(q::setParameter);
        return q;
    }
}
