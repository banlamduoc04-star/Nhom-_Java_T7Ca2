package com.oism.application.order;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.oism.domain.repository.TenantRepository;
import com.oism.domain.service.OrderStateMachine;

import jakarta.persistence.criteria.Predicate;

/**
 * Xử lý toàn bộ vòng đời đơn hàng (FR-ORD-01..03, FR-RSE-01..03, FR-COST-02):
 * Tạo Draft -> Giữ hàng (Reserved) -> Xác nhận trừ kho chụp giá vốn (Confirmed)
 * -> Hoàn tất (Completed) | Hủy (Cancelled).
 */
@Service
public class OrderService {

    public static final Duration DEFAULT_RESERVE_DURATION = Duration.ofMinutes(15);

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final TenantRepository tenantRepository;
    private final ReservationService reservationService;
    private final SecureRandom random = new SecureRandom();

    public OrderService(SalesOrderRepository orderRepository,
                        OrderItemRepository itemRepository,
                        OrderStatusHistoryRepository historyRepository,
                        BranchRepository branchRepository,
                        SkuRepository skuRepository,
                        ReservationService reservationService,
                        TenantRepository tenantRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
        this.reservationService = reservationService;
        this.tenantRepository = tenantRepository;
    }

    // =========================================================================
    // FR-ORD-01 + FR-ORD-03: Tạo đơn
    // =========================================================================

    @Transactional
    public OrderView createDraft(UUID tenantId, UUID userId, UUID branchId, String customerName,
                                 String customerPhone, String shippingAddress, List<OrderLine> lines) {
        assertBranchActive(tenantId, branchId);
        assertLinesValid(lines);

        Map<UUID, Sku> skus = loadSkus(tenantId, lines);
        BigDecimal total = calculateTotal(lines, skus);

        OrderStateMachine.assertTransition(null, OrderStatus.DRAFT);
        SalesOrder order = orderRepository.save(new SalesOrder(
                tenantId, branchId, newCode(tenantId), OrderChannel.MANUAL, null,
                OrderStatus.DRAFT, blankToNull(customerName), blankToNull(customerPhone),
                blankToNull(shippingAddress), total, userId));

        saveOrderItems(tenantId, order.getId(), lines, skus);
        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), null, OrderStatus.DRAFT,
                userId, "Tạo đơn nháp thủ công"));
        return view(tenantId, order);
    }

    @Transactional
    public OrderView createAndReserve(UUID tenantId, UUID userId, UUID branchId, OrderChannel channel,
                                      String externalOrderId, String customerName, String customerPhone,
                                      String shippingAddress, List<OrderLine> lines) {
        assertBranchActive(tenantId, branchId);
        assertLinesValid(lines);
        assertNoDuplicateExternalOrder(tenantId, channel, externalOrderId);

        Map<UUID, Sku> skus = loadSkus(tenantId, lines);
        BigDecimal total = calculateTotal(lines, skus);

        SalesOrder order = orderRepository.save(new SalesOrder(
                tenantId, branchId, newCode(tenantId), channel, blankToNull(externalOrderId),
                OrderStatus.DRAFT, blankToNull(customerName), blankToNull(customerPhone),
                blankToNull(shippingAddress), total, userId));

        List<OrderItem> items = saveOrderItems(tenantId, order.getId(), lines, skus);

        OrderStateMachine.assertTransition(OrderStatus.DRAFT, OrderStatus.RESERVED);
        reservationService.reserve(tenantId, branchId, items);
        order.moveTo(OrderStatus.RESERVED);
        order.setReservedUntil(Instant.now().plus(getReservationDuration(tenantId)));
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), null, OrderStatus.DRAFT,
                userId, "Tạo đơn"));
        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), OrderStatus.DRAFT,
                OrderStatus.RESERVED, userId, "Giữ hàng thành công"));

        return view(tenantId, order);
    }

    // =========================================================================
    // FR-ORD-02: Chuyển trạng thái đơn hàng
    // =========================================================================

    @Transactional
    public OrderView reserve(UUID tenantId, UUID userId, UUID orderId) {
        SalesOrder order = find(tenantId, orderId);
        OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.RESERVED);

        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, orderId);
        reservationService.reserve(tenantId, order.getBranchId(), items);

        OrderStatus from = order.getStatus();
        order.moveTo(OrderStatus.RESERVED);
        order.setReservedUntil(Instant.now().plus(getReservationDuration(tenantId)));
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), from, OrderStatus.RESERVED,
                userId, "Giữ hàng thành công"));
        return view(tenantId, order);
    }

    @Transactional
    public OrderView confirm(UUID tenantId, UUID userId, UUID orderId) {
        SalesOrder order = find(tenantId, orderId);
        OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.CONFIRMED);

        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, orderId);
        reservationService.confirm(tenantId, order.getBranchId(), orderId, items, userId);

        OrderStatus from = order.getStatus();
        order.moveTo(OrderStatus.CONFIRMED);
        order.setReservedUntil(null);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), from, OrderStatus.CONFIRMED,
                userId, "Duyệt đơn và trừ kho"));
        return view(tenantId, order);
    }

    @Transactional
    public OrderView complete(UUID tenantId, UUID userId, UUID orderId) {
        SalesOrder order = find(tenantId, orderId);
        OrderStateMachine.assertTransition(order.getStatus(), OrderStatus.COMPLETED);

        OrderStatus from = order.getStatus();
        order.moveTo(OrderStatus.COMPLETED);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), from, OrderStatus.COMPLETED,
                userId, "Giao hàng và hoàn tất đơn"));
        return view(tenantId, order);
    }

    @Transactional
    public OrderView cancel(UUID tenantId, UUID userId, UUID orderId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.OD40001, "Hủy đơn bắt buộc nhập lý do");
        }
        SalesOrder order = find(tenantId, orderId);
        OrderStatus from = order.getStatus();
        OrderStateMachine.assertTransition(from, OrderStatus.CANCELLED);

        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, orderId);
        if (from == OrderStatus.RESERVED) {
            reservationService.release(tenantId, order.getBranchId(), items);
        } else if (from == OrderStatus.CONFIRMED) {
            reservationService.returnStock(tenantId, order.getBranchId(), orderId, items, userId);
        }

        order.moveTo(OrderStatus.CANCELLED);
        order.setReservedUntil(null);
        orderRepository.save(order);

        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), from, OrderStatus.CANCELLED,
                userId, reason.trim()));
        return view(tenantId, order);
    }

    // =========================================================================
    // FR-SIM-03: Tác vụ nền - tự động hủy đơn quá hạn
    // =========================================================================

    @Transactional
    public int cancelExpiredReservations(UUID tenantId) {
        List<SalesOrder> expired = orderRepository.findByTenantIdAndStatusAndReservedUntilBefore(
                tenantId, OrderStatus.RESERVED, Instant.now());
        for (SalesOrder o : expired) {
            List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, o.getId());
            reservationService.release(tenantId, o.getBranchId(), items);
            o.moveTo(OrderStatus.CANCELLED);
            o.setReservedUntil(null);
            orderRepository.save(o);
            historyRepository.save(new OrderStatusHistory(tenantId, o.getId(), OrderStatus.RESERVED,
                    OrderStatus.CANCELLED, null, "Quá hạn giữ hàng (tự động nhả tồn)"));
        }
        return expired.size();
    }

    // =========================================================================
    // Truy vấn đơn hàng
    // =========================================================================

    @Transactional(readOnly = true)
    public OrderView get(UUID tenantId, UUID id) {
        return view(tenantId, find(tenantId, id));
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
            return cb.and(ps.toArray(Predicate[]::new));
        };
        Page<SalesOrder> result = orderRepository.findAll(spec,
                Paging.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResult.of(result, o -> view(tenantId, o));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private SalesOrder find(UUID tenantId, UUID orderId) {
        return orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40401, "Không tìm thấy đơn hàng"));
    }

    private void assertBranchActive(UUID tenantId, UUID branchId) {
        branchRepository.findByIdAndTenantId(branchId, tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40001, "Chi nhánh không hợp lệ"));
    }

    private void assertLinesValid(List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new BusinessException(ErrorCode.OD40001, "Đơn hàng cần ít nhất 1 dòng hàng");
        }
    }

    private void assertNoDuplicateExternalOrder(UUID tenantId, OrderChannel channel, String externalOrderId) {
        if (externalOrderId != null && !externalOrderId.isBlank()) {
            if (orderRepository.findByTenantIdAndChannelAndExternalOrderId(tenantId, channel, externalOrderId.trim()).isPresent()) {
                throw new BusinessException(ErrorCode.OD40902,
                        "Đơn hàng từ sàn " + channel + " với mã " + externalOrderId + " đã tồn tại");
            }
        }
    }

    private Map<UUID, Sku> loadSkus(UUID tenantId, List<OrderLine> lines) {
        List<UUID> skuIds = lines.stream().map(OrderLine::skuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));
        if (skus.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.PR40401, "Có SKU không tồn tại");
        }
        return skus;
    }

    private BigDecimal calculateTotal(List<OrderLine> lines, Map<UUID, Sku> skus) {
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
        return total;
    }

    private List<OrderItem> saveOrderItems(UUID tenantId, UUID orderId, List<OrderLine> lines, Map<UUID, Sku> skus) {
        List<OrderItem> items = lines.stream()
                .map(l -> new OrderItem(tenantId, orderId, l.skuId(), l.quantity(),
                        skus.get(l.skuId()).getRetailPrice()))
                .toList();
        itemRepository.saveAll(items);
        return items;
    }

    private OrderView view(UUID tenantId, SalesOrder o) {
        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, o.getId());
        Map<UUID, String> codes = skuRepository.findByTenantIdAndIdIn(tenantId,
                items.stream().map(OrderItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Sku::getCode));

        List<OrderView.Item> itemViews = items.stream()
                .map(i -> new OrderView.Item(i.getSkuId(), codes.get(i.getSkuId()), i.getQuantity(),
                        i.getUnitPrice(), i.getCostPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                .toList();

        List<OrderView.History> history = historyRepository
                .findByTenantIdAndOrderIdOrderByCreatedAtAsc(tenantId, o.getId()).stream()
                .map(h -> new OrderView.History(h.getFromStatus(), h.getToStatus(), h.getChangedBy(),
                        h.getReason(), h.getCreatedAt()))
                .toList();

        return new OrderView(o.getId(), o.getCode(), o.getBranchId(), o.getChannel(), o.getExternalOrderId(),
                o.getStatus(), o.getCustomerName(), o.getCustomerPhone(), o.getShippingAddress(),
                o.getTotalAmount(), o.getReservedUntil(), o.getCreatedAt(), itemViews, history);
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

    private Duration getReservationDuration(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> Duration.ofMinutes(t.getReservationMinutes()))
                .orElse(DEFAULT_RESERVE_DURATION);
    }
}