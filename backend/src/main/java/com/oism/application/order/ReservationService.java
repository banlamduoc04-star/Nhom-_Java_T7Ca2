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
 * Cơ chế giữ hàng và trừ/nhả hàng chống bán vượt (FR-RSE-01..03, NFR-PERF-02).
 * Khóa dòng stock_balance theo thứ tự sku_id tăng dần để ngăn ngừa deadlock tuyệt đối.
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
     * Giữ hàng (Draft -> Reserved):
     * Sắp xếp theo skuId tăng dần, khóa FOR UPDATE từng dòng,
     * kiểm tra available = on_hand - reserved >= quantity, nếu không đủ throw IV40901.
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
     * Nhả hàng giữ (Reserved -> Cancelled): giảm reserved của từng SKU theo thứ tự skuId tăng dần.
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
     * Xác nhận đơn (Reserved -> Confirmed):
     * BƯỚC 1: Chụp cost_price (avg_cost) vào order_item (FR-COST-02).
     * BƯỚC 2: Gọi ledgerService.post() để trừ on_hand (hàm này check available >= quantity).
     * BƯỚC 3: Giảm reserved (SAU KHI trừ on_hand thành công).
     * TUYỆT ĐỐI KHÔNG giảm reserved trước vì sẽ vô hiệu hóa check chống bán vượt.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirm(UUID tenantId, UUID branchId, UUID orderId, List<OrderItem> items, UUID userId) {
        List<OrderItem> sorted = items.stream()
                .sorted(Comparator.comparing(OrderItem::getSkuId))
                .toList();

        for (OrderItem item : sorted) {
            // BƯỚC 1: Chụp giá vốn hiện tại (avg_cost) vào order_item
            Sku sku = skuRepository.findForUpdate(item.getSkuId(), tenantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PR40401, "Không tìm thấy SKU"));
            BigDecimal costPrice = sku.getAvgCost();
            item.setCostPrice(costPrice);
            orderItemRepository.save(item);

            // BƯỚC 2: Ghi sổ cái OUT/SALE - hàm này check available >= quantity và trừ on_hand
            ledgerService.post(new LedgerEntry(
                    tenantId, branchId, item.getSkuId(), StockDirection.OUT, StockReason.SALE,
                    item.getQuantity(), costPrice, REF_TYPE_ORDER, orderId, userId
            ));

            // BƯỚC 3: Chỉ sau khi trừ on_hand thành công mới giảm reserved
            StockBalance balance = stockBalanceRepository.findForUpdate(tenantId, branchId, item.getSkuId())
                    .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));
            balance.applyReserved(Math.max(0, balance.getReserved() - item.getQuantity()));
            stockBalanceRepository.save(balance);
        }
    }

    /**
     * Hoàn trả hàng (Confirmed -> Cancelled): ghi sổ cái IN với StockReason.RETURN để bù lại tồn kho.
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