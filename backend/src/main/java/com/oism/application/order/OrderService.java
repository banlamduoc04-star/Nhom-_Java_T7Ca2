package com.oism.application.order;

import com.oism.application.common.PageResult;
import com.oism.application.common.Paging;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.OrderStatusHistory;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.OrderStatusHistoryRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.service.OrderStateMachine;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FR-ORD-01..03 (Sprint 2): tao don tay o trang thai Draft, xem danh sach/chi tiet kem lich su.
 * Giu hang, tru hang, huy... (RSE) lam o Sprint 3 tren cung may trang thai.
 */
@Service
public class OrderService {

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final SecureRandom random = new SecureRandom();

    public OrderService(SalesOrderRepository orderRepository,
                        OrderItemRepository itemRepository,
                        OrderStatusHistoryRepository historyRepository,
                        BranchRepository branchRepository,
                        SkuRepository skuRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
    }

    /** POST /orders: tao don tay (Draft). Don gia chup tu gia ban le hien tai cua SKU (FR-PROD-04). */
    @Transactional
    public OrderView createDraft(UUID tenantId, UUID userId, UUID branchId, String customerName,
                                 String customerPhone, String shippingAddress, List<OrderLine> lines) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40001, "Chi nhánh không hợp lệ"));
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.OD40001, "Đơn hàng cần ít nhất 1 dòng hàng");
        }
        List<UUID> skuIds = lines.stream().map(OrderLine::skuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        if (skus.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.PR40401, "Có SKU không tồn tại");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            if (line.quantity() <= 0) {
                throw new BusinessException(ErrorCode.OD40001, "Số lượng phải lớn hơn 0");
            }
            Sku sku = skus.get(line.skuId());
            if (sku.getStatus() != CatalogStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.OD40001, "SKU " + sku.getCode() + " đã ngừng bán");
            }
            total = total.add(sku.getRetailPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        OrderStateMachine.assertTransition(null, OrderStatus.DRAFT);
        SalesOrder order = orderRepository.save(new SalesOrder(tenantId, branchId, newCode(tenantId),
                OrderChannel.MANUAL, null, OrderStatus.DRAFT, blankToNull(customerName), blankToNull(customerPhone),
                blankToNull(shippingAddress), total, userId));
        itemRepository.saveAll(lines.stream()
                .map(l -> new OrderItem(tenantId, order.getId(), l.skuId(), l.quantity(),
                        skus.get(l.skuId()).getRetailPrice()))
                .toList());
        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), null, OrderStatus.DRAFT, userId, null));
        return view(tenantId, order);
    }

    @Transactional(readOnly = true)
    public OrderView get(UUID tenantId, UUID id) {
        SalesOrder order = orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40401, "Không tìm thấy đơn hàng"));
        return view(tenantId, order);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderView> list(UUID tenantId, OrderStatus status, OrderChannel channel, UUID branchId,
                                      Instant from, Instant to, Integer page, Integer size) {
        Specification<SalesOrder> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (channel != null) {
                ps.add(cb.equal(root.get("channel"), channel));
            }
            if (branchId != null) {
                ps.add(cb.equal(root.get("branchId"), branchId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Page<SalesOrder> result = orderRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result, o -> view(tenantId, o));
    }

    private OrderView view(UUID tenantId, SalesOrder o) {
        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, o.getId());
        Map<UUID, String> codes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(OrderItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));
        List<OrderView.Item> itemViews = items.stream()
                .map(i -> new OrderView.Item(i.getSkuId(), codes.get(i.getSkuId()), i.getQuantity(), i.getUnitPrice(),
                        i.getCostPrice(), i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();
        List<OrderView.History> history = historyRepository
                .findByTenantIdAndOrderIdOrderByCreatedAtAsc(tenantId, o.getId()).stream()
                .map(h -> new OrderView.History(h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getReason(),
                        h.getCreatedAt()))
                .toList();
        return new OrderView(o.getId(), o.getCode(), o.getBranchId(), o.getChannel(), o.getExternalOrderId(),
                o.getStatus(), o.getCustomerName(), o.getCustomerPhone(), o.getShippingAddress(), o.getTotalAmount(),
                o.getReservedUntil(), o.getCreatedAt(), itemViews, history);
    }

    private String newCode(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int i = 0; i < 20; i++) {
            String code = "DH-" + date + "-" + String.format("%04d", random.nextInt(10_000));
            if (!orderRepository.existsByTenantIdAndCode(tenantId, code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không sinh được mã đơn hàng");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
