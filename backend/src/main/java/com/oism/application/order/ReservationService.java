package com.oism.application.order;

import com.oism.application.inventory.InventoryLedgerService;
import com.oism.application.inventory.LedgerEntry;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final String REF_TYPE_ORDER = "ORDER";

    private final StockBalanceRepository stockBalanceRepository;
    private final SkuRepository skuRepository;
    private final InventoryLedgerService ledgerService;
    private final OrderItemRepository orderItemRepository;

    public ReservationService(StockBalanceRepository stockBalanceRepository,
                              SkuRepository skuRepository,
                              InventoryLedgerService ledgerService,
                              OrderItemRepository orderItemRepository) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.skuRepository = skuRepository;
        this.ledgerService = ledgerService;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void reserve(UUID tenantId, UUID branchId, List<OrderItem> items) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            stockBalanceRepository.insertIfAbsent(tenantId, branchId, item.getSkuId());
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));

            if (balance.getAvailable() < item.getQuantity()) {
                Sku sku = skuRepository.findByIdAndTenantId(item.getSkuId(), tenantId).orElse(null);
                String skuCode = sku != null ? sku.getCode() : item.getSkuId().toString();
                throw new BusinessException(ErrorCode.IV40901,
                        "Không đủ hàng khả dụng cho SKU " + skuCode + " (còn " + balance.getAvailable() + ", yêu cầu " + item.getQuantity() + ")");
            }

            balance.applyReserved(balance.getReserved() + item.getQuantity());
            stockBalanceRepository.save(balance);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(UUID tenantId, UUID branchId, List<OrderItem> items) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));

            balance.applyReserved(Math.max(0, balance.getReserved() - item.getQuantity()));
            stockBalanceRepository.save(balance);
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(UUID tenantId, UUID branchId, UUID orderId, List<OrderItem> items, UUID userId) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));

            balance.applyReserved(Math.max(0, balance.getReserved() - item.getQuantity()));
            stockBalanceRepository.save(balance);

            Sku sku = skuRepository.findForUpdate(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));
            BigDecimal costPrice = sku.getAvgCost();
            item.setCostPrice(costPrice);
            orderItemRepository.save(item);

            ledgerService.post(new LedgerEntry(
                    tenantId, branchId, item.getSkuId(), StockDirection.OUT, StockReason.SALE,
                    item.getQuantity(), costPrice, REF_TYPE_ORDER, orderId, userId
            ));
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void returnStock(UUID tenantId, UUID branchId, UUID orderId, List<OrderItem> items, UUID userId) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            BigDecimal costPrice = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
            ledgerService.post(new LedgerEntry(
                    tenantId, branchId, item.getSkuId(), StockDirection.IN, StockReason.RETURN,
                    item.getQuantity(), costPrice, REF_TYPE_ORDER, orderId, userId
            ));
        }
    }
}
