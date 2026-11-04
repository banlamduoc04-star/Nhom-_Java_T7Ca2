package com.oism.application.order;

import com.oism.application.inventory.StockAlertService;
import com.oism.domain.event.OrderCreatedEvent;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.OrderStatusHistory;
import com.oism.domain.model.Payment;
import com.oism.domain.model.PaymentMethod;
import com.oism.domain.model.Product;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.OrderStatusHistoryRepository;
import com.oism.domain.repository.PaymentRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.service.OrderStateMachine;
import org.springframework.context.ApplicationEventPublisher;
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
 * Xu ly thanh toan POS trong dung mot transaction (FR-POS-04, JC-31):
 * Tao don -> Giu hang -> Xac nhan tru kho -> Hoan tat -> Ghi payment.
 */
@Service
public class PosCheckoutService {

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final PaymentRepository paymentRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;
    private final ReservationService reservationService;
    private final StockAlertService stockAlertService;
    private final ApplicationEventPublisher eventPublisher;
    private final SecureRandom random = new SecureRandom();

    public PosCheckoutService(SalesOrderRepository orderRepository,
                              OrderItemRepository itemRepository,
                              OrderStatusHistoryRepository historyRepository,
                              PaymentRepository paymentRepository,
                              BranchRepository branchRepository,
                              SkuRepository skuRepository,
                              ProductRepository productRepository,
                              ReservationService reservationService,
                              StockAlertService stockAlertService,
                              ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.historyRepository = historyRepository;
        this.paymentRepository = paymentRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
        this.reservationService = reservationService;
        this.stockAlertService = stockAlertService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PosCheckoutResponse checkout(UUID tenantId, UUID userId, String cashierName, PosCheckoutCommand cmd) {
        Branch branch = branchRepository.findByIdAndTenantId(cmd.branchId(), tenantId)
                .filter(Branch::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40001, "Chi nhánh không hợp lệ hoặc đã bị khóa"));

        if (cmd.lines() == null || cmd.lines().isEmpty()) {
            throw new BusinessException(ErrorCode.OD40001, "Giỏ hàng không có sản phẩm");
        }

        List<UUID> skuIds = cmd.lines().stream().map(PosCheckoutCommand.PosLine::skuId).distinct().toList();
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId, skuIds).stream()
                .collect(Collectors.toMap(Sku::getId, s -> s));

        if (skus.size() != skuIds.size()) {
            throw new BusinessException(ErrorCode.PR40401, "Một số sản phẩm không tồn tại");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (PosCheckoutCommand.PosLine line : cmd.lines()) {
            if (line.quantity() <= 0) {
                throw new BusinessException(ErrorCode.OD40001, "Số lượng sản phẩm phải lớn hơn 0");
            }
            Sku sku = skus.get(line.skuId());
            if (sku.getStatus() != CatalogStatus.ACTIVE) {
                throw new BusinessException(ErrorCode.OD40001, "SKU " + sku.getCode() + " đã ngừng kinh doanh");
            }
            total = total.add(sku.getRetailPrice().multiply(BigDecimal.valueOf(line.quantity())));
        }

        // Kiem tra tien khach dua
        BigDecimal cashReceived;
        BigDecimal changeAmount;
        if (cmd.paymentMethod() == PaymentMethod.CASH) {
            cashReceived = cmd.cashReceived() != null ? cmd.cashReceived() : total;
            if (cashReceived.compareTo(total) < 0) {
                throw new BusinessException(ErrorCode.OD40001, "Tiền khách đưa nhỏ hơn tổng tiền đơn hàng");
            }
            changeAmount = cashReceived.subtract(total);
        } else {
            cashReceived = total;
            changeAmount = BigDecimal.ZERO;
        }

        // 1. Tao don POS o trang thai DRAFT
        String orderCode = newCode(tenantId);
        SalesOrder order = orderRepository.save(new SalesOrder(
                tenantId, cmd.branchId(), orderCode, OrderChannel.POS, null,
                OrderStatus.DRAFT, cmd.customerName(), cmd.customerPhone(), null, total, userId));

        List<OrderItem> items = cmd.lines().stream()
                .map(l -> new OrderItem(tenantId, order.getId(), l.skuId(), l.quantity(),
                        skus.get(l.skuId()).getRetailPrice()))
                .toList();
        itemRepository.saveAll(items);

        // 2. Giu hang (RESERVED)
        OrderStateMachine.assertTransition(OrderStatus.DRAFT, OrderStatus.RESERVED);
        reservationService.reserve(tenantId, cmd.branchId(), items);
        order.moveTo(OrderStatus.RESERVED);

        // 3. Xac nhan tru kho (CONFIRMED) & chup gia von
        OrderStateMachine.assertTransition(OrderStatus.RESERVED, OrderStatus.CONFIRMED);
        reservationService.confirm(tenantId, cmd.branchId(), order.getId(), items, userId);
        order.moveTo(OrderStatus.CONFIRMED);

        // 4. Hoan tat don hang (COMPLETED)
        OrderStateMachine.assertTransition(OrderStatus.CONFIRMED, OrderStatus.COMPLETED);
        order.moveTo(OrderStatus.COMPLETED);
        orderRepository.save(order);

        // 5. Ghi nhan thanh toan
        paymentRepository.save(new Payment(tenantId, order.getId(), cmd.paymentMethod(), total, cashReceived, changeAmount));

        // 6. Ghi lich su don
        historyRepository.save(new OrderStatusHistory(tenantId, order.getId(), null, OrderStatus.COMPLETED,
                userId, "Thanh toán POS thành công"));

        // 7. Kiem tra canh bao ton thap cho tung SKU
        for (UUID skuId : skuIds) {
            try {
                stockAlertService.checkStockLevel(tenantId, cmd.branchId(), skuId);
            } catch (Exception e) {
                // Khong lam fail checkout neu alert loi
            }
        }

        // 8. Phat su kien realtime
        eventPublisher.publishEvent(new OrderCreatedEvent(
                tenantId, order.getId(), order.getCode(), total, OrderChannel.POS.name(), cashierName, Instant.now()));

        // 9. Tra ve response in hoa don
        List<UUID> prodIds = skus.values().stream().map(Sku::getProductId).distinct().toList();
        Map<UUID, String> prodNames = productRepository.findAllById(prodIds).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<PosCheckoutResponse.PosReceiptLine> receiptItems = items.stream().map(it -> {
            Sku sku = skus.get(it.getSkuId());
            String pName = prodNames.getOrDefault(sku.getProductId(), sku.getCode());
            BigDecimal lineTotal = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            return new PosCheckoutResponse.PosReceiptLine(it.getSkuId(), sku.getCode(), pName, it.getQuantity(), it.getUnitPrice(), lineTotal);
        }).toList();

        return new PosCheckoutResponse(
                order.getId(),
                order.getCode(),
                order.getStatus(),
                branch.getId(),
                branch.getName(),
                total,
                cmd.paymentMethod(),
                cashReceived,
                changeAmount,
                Instant.now(),
                cashierName,
                receiptItems
        );
    }

    private String newCode(UUID tenantId) {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyMMdd"));
        for (int i = 0; i < 10; i++) {
            String candidate = String.format("POS-%s-%04d", date, random.nextInt(10_000));
            if (!orderRepository.existsByTenantIdAndCode(tenantId, candidate)) {
                return candidate;
            }
        }
        return "POS-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
