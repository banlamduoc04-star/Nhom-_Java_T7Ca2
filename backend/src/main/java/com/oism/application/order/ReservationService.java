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

/**
 * Co che giu hang va tru/nha hang chong ban vuot (FR-RSE-01..03, NFR-PERF-02).
 * Khoa dong stock_balance theo thu tu sku_id tang dan de ngan ngua deadlock tuyet doi.
 */
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

    /**
     * Giu hang (Draft -> Reserved): sap xep theo skuId tang dan, khoa FOR UPDATE tung dong,
     * kiem tra available = on_hand - reserved >= quantity, neu khong du throw IV40901.
     */
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

    /**
     * Nha hang giu (Reserved -> Cancelled): giam reserved cua tung SKU theo thu tu skuId tang dan.
     */
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

    /**
     * Xac nhan don (Reserved -> Confirmed): nha reserved, ghi so cai OUT_SALE qua InventoryLedgerService,
     * va chup cost_price tu avg_cost hien tai cua SKU vao order_item (FR-COST-02).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(UUID tenantId, UUID branchId, UUID orderId, List<OrderItem> items, UUID userId) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));

            // Giam reserved truoc de tang lai available truoc khi ledgerService.post kiem tra available
            balance.applyReserved(Math.max(0, balance.getReserved() - item.getQuantity()));
            stockBalanceRepository.save(balance);

            // Lay avg_cost hien tai cua SKU de chup vao don hang (FR-COST-02)
            Sku sku = skuRepository.findForUpdate(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));

            BigDecimal costPrice = sku.getAvgCost();
            item.setCostPrice(costPrice);
            orderItemRepository.save(item);

            // Ghi but toan xuat ban hang vao so cai
            ledgerService.post(new LedgerEntry(
                    tenantId, branchId, item.getSkuId(), StockDirection.OUT, StockReason.SALE,
                    item.getQuantity(), costPrice, REF_TYPE_ORDER, orderId, userId
            ));
        }
    }

    /**
     * Hoan tra hang (Confirmed -> Cancelled): ghi so cai IN voi StockReason.RETURN de bu lai ton kho.
     */
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
