package com.oism.application.order;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.Product;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Phieu giao hang (FR-ORD-03 "in phieu giao hang"): du lieu tu don, chi nhanh, SKU, san pham cung tenant. */
@Service
public class ShippingSlipService {

    private final SalesOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final BranchRepository branchRepository;
    private final SkuRepository skuRepository;
    private final ProductRepository productRepository;

    public ShippingSlipService(SalesOrderRepository orderRepository,
                               OrderItemRepository itemRepository,
                               BranchRepository branchRepository,
                               SkuRepository skuRepository,
                               ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.branchRepository = branchRepository;
        this.skuRepository = skuRepository;
        this.productRepository = productRepository;
    }

    /** Khong thay hoac khac tenant -> 404 OD40401; don da huy khong giao -> 409 OD40901. */
    @Transactional(readOnly = true)
    public ShippingSlipView shippingSlip(UUID tenantId, UUID orderId) {
        SalesOrder order = orderRepository.findByIdAndTenantId(orderId, tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OD40401, "Không tìm thấy đơn hàng"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.OD40901, "Đơn đã hủy, không in phiếu giao hàng");
        }
        Branch branch = branchRepository.findByIdAndTenantId(order.getBranchId(), tenantId).orElse(null);

        List<OrderItem> items = itemRepository.findByTenantIdAndOrderId(tenantId, orderId);
        Map<UUID, Sku> skus = skuRepository.findByTenantIdAndIdIn(tenantId,
                        items.stream().map(OrderItem::getSkuId).distinct().toList()).stream()
                .collect(Collectors.toMap(Sku::getId, Function.identity()));
        Map<UUID, String> productNames = productRepository.findByTenantIdAndIdIn(tenantId,
                        skus.values().stream().map(Sku::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName, (a, b) -> a));

        List<ShippingSlipView.Line> lines = items.stream().map(it -> {
            Sku sku = skus.get(it.getSkuId());
            String skuCode = sku != null ? sku.getCode() : it.getSkuId().toString();
            String productName = sku != null ? productNames.getOrDefault(sku.getProductId(), skuCode) : skuCode;
            return new ShippingSlipView.Line(skuCode, productName, it.getQuantity(), it.getUnitPrice(),
                    it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())));
        }).toList();

        return new ShippingSlipView(order.getId(), order.getCode(), order.getChannel(), order.getExternalOrderId(),
                order.getStatus(), branch != null ? branch.getName() : null, branch != null ? branch.getAddress() : null,
                order.getCustomerName(), order.getCustomerPhone(), order.getShippingAddress(), order.getCreatedAt(),
                order.getTotalAmount(), lines);
    }
}
