package com.oism.application.order;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.OrderChannel;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.OrderStatusHistoryRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock SalesOrderRepository orderRepository;
    @Mock OrderItemRepository itemRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock BranchRepository branchRepository;
    @Mock SkuRepository skuRepository;
    @Mock ReservationService reservationService;
    @Mock com.oism.domain.repository.TenantRepository tenantRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;

    OrderService service;

    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new OrderService(orderRepository, itemRepository, historyRepository,
                branchRepository, skuRepository, reservationService, tenantRepository, eventPublisher);

        Branch branch = new Branch(tenantId, "Chi nhánh 1", BranchType.STORE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByIdAndTenantId(branchId, tenantId)).thenReturn(Optional.of(branch));

        Sku sku = new Sku(tenantId, UUID.randomUUID(), "TSHIRT-RED", Map.of(), null, BigDecimal.valueOf(150), null, 0);
        ReflectionTestUtils.setField(sku, "id", skuId);
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));
        when(skuRepository.findByIdAndTenantId(skuId, tenantId)).thenReturn(Optional.of(sku));

        when(orderRepository.save(any(SalesOrder.class))).thenAnswer(inv -> {
            SalesOrder order = inv.getArgument(0);
            if (order.getId() == null) {
                ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
            }
            return order;
        });
    }

    @Test
    void createAndReserve_success() {
        List<OrderLine> lines = List.of(new OrderLine(skuId, 2));

        OrderView view = service.createAndReserve(tenantId, userId, branchId, OrderChannel.POS,
                null, "Khách lẻ", "0901234567", null, lines);

        assertNotNull(view);
        assertEquals(OrderStatus.RESERVED, view.status());
        verify(reservationService).reserve(any(), any(), any());
    }

    @Test
    void createAndReserve_failsWhenReservationThrows_transactionRollback() {
        List<OrderLine> lines = List.of(new OrderLine(skuId, 100));

        doThrow(new BusinessException(ErrorCode.IV40901, "Không đủ hàng khả dụng"))
                .when(reservationService).reserve(any(), any(), any());

        assertThrows(BusinessException.class, () ->
                service.createAndReserve(tenantId, userId, branchId, OrderChannel.SHOPEE,
                        "EXT-001", "Khách Shopee", null, null, lines));
    }

    @Test
    void confirm_callsReservationServiceConfirm() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, branchId, "DH-001", OrderChannel.POS, null,
                OrderStatus.RESERVED, "Khách", null, null, BigDecimal.valueOf(300), userId);
        ReflectionTestUtils.setField(order, "id", orderId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 2, BigDecimal.valueOf(150));
        when(itemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item));

        OrderView view = service.confirm(tenantId, userId, orderId);

        assertEquals(OrderStatus.CONFIRMED, view.status());
        verify(reservationService).confirm(tenantId, branchId, orderId, List.of(item), userId);
    }

    @Test
    void cancel_releasesReservation_whenOrderWasReserved() {
        UUID orderId = UUID.randomUUID();
        SalesOrder order = new SalesOrder(tenantId, branchId, "DH-001", OrderChannel.POS, null,
                OrderStatus.RESERVED, "Khách", null, null, BigDecimal.valueOf(300), userId);
        ReflectionTestUtils.setField(order, "id", orderId);
        when(orderRepository.findByIdAndTenantId(orderId, tenantId)).thenReturn(Optional.of(order));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 2, BigDecimal.valueOf(150));
        when(itemRepository.findByTenantIdAndOrderId(tenantId, orderId)).thenReturn(List.of(item));

        OrderView view = service.cancel(tenantId, userId, orderId, "Khách hủy");

        assertEquals(OrderStatus.CANCELLED, view.status());
        verify(reservationService).release(tenantId, branchId, List.of(item));
    }

    @Test
    void createAndReserve_fromMarketplace_publishesOrderCreatedEvent() {
        service.createAndReserve(tenantId, null, branchId, OrderChannel.SHOPEE,
                "SP-777", "Khách Shopee", null, null, List.of(new OrderLine(skuId, 1)));

        org.mockito.ArgumentCaptor<com.oism.domain.event.OrderCreatedEvent> event =
                org.mockito.ArgumentCaptor.forClass(com.oism.domain.event.OrderCreatedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertEquals(tenantId, event.getValue().tenantId());
        assertEquals("SHOPEE", event.getValue().channel());
    }

    @Test
    void createDraft_publishesOrderCreatedEvent_butFailedReservationDoesNot() {
        service.createDraft(tenantId, userId, branchId, "Khách", null, null, List.of(new OrderLine(skuId, 1)));
        verify(eventPublisher).publishEvent(any(com.oism.domain.event.OrderCreatedEvent.class));

        doThrow(new BusinessException(ErrorCode.IV40901, "Không đủ hàng khả dụng"))
                .when(reservationService).reserve(any(), any(), any());
        assertThrows(BusinessException.class, () -> service.createAndReserve(tenantId, null, branchId,
                OrderChannel.TIKTOK, "TT-9", null, null, null, List.of(new OrderLine(skuId, 99))));
        verify(eventPublisher, org.mockito.Mockito.times(1)).publishEvent(any(Object.class));
    }
}
