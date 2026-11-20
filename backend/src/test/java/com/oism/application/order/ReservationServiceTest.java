package com.oism.application.order;

import com.oism.application.inventory.InventoryLedgerService;
import com.oism.application.inventory.StockAlertService;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.OrderItem;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock StockBalanceRepository stockBalanceRepository;
    @Mock SkuRepository skuRepository;
    @Mock InventoryLedgerService ledgerService;
    @Mock OrderItemRepository orderItemRepository;
    @Mock StockAlertService stockAlertService;

    ReservationService reservationService;

    UUID tenantId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        reservationService = new ReservationService(stockBalanceRepository, skuRepository, ledgerService, orderItemRepository,
                stockAlertService);
    }

    private StockBalance createBalance(int onHand, int reserved) {
        try {
            Constructor<StockBalance> c = StockBalance.class.getDeclaredConstructor();
            c.setAccessible(true);
            StockBalance balance = c.newInstance();
            ReflectionTestUtils.setField(balance, "tenantId", tenantId);
            ReflectionTestUtils.setField(balance, "branchId", branchId);
            ReflectionTestUtils.setField(balance, "skuId", skuId);
            ReflectionTestUtils.setField(balance, "onHand", onHand);
            ReflectionTestUtils.setField(balance, "reserved", reserved);
            return balance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void reserve_success_whenAvailableIsSufficient() {
        StockBalance balance = createBalance(10, 2); // available = 8
        when(stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 5, BigDecimal.valueOf(100));

        reservationService.reserve(tenantId, branchId, List.of(item));

        assertEquals(7, balance.getReserved());
        assertEquals(3, balance.getAvailable());
        verify(stockBalanceRepository).save(balance);
    }

    @Test
    void reserve_throwsIV40901_whenAvailableIsInsufficient() {
        StockBalance balance = createBalance(5, 4); // available = 1
        when(stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 2, BigDecimal.valueOf(100));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> reservationService.reserve(tenantId, branchId, List.of(item)));

        assertEquals(ErrorCode.IV40901, ex.getErrorCode());
    }

    @Test
    void release_decrementsReserved() {
        StockBalance balance = createBalance(10, 5);
        when(stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 3, BigDecimal.valueOf(100));

        reservationService.release(tenantId, branchId, List.of(item));

        assertEquals(2, balance.getReserved());
        verify(stockBalanceRepository).save(balance);
    }

    @Test
    void confirm_releasesReserved_capturesAvgCost_andPostsLedger() {
        StockBalance balance = createBalance(10, 3);
        when(stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        Sku sku = new Sku(tenantId, UUID.randomUUID(), "POLO-M", Map.of(), null, BigDecimal.valueOf(100), null, 0);
        sku.setAvgCost(BigDecimal.valueOf(65.50));
        when(skuRepository.findForUpdate(skuId, tenantId)).thenReturn(Optional.of(sku));

        OrderItem item = new OrderItem(tenantId, orderId, skuId, 3, BigDecimal.valueOf(100));

        reservationService.confirm(tenantId, branchId, orderId, List.of(item), userId);

        assertEquals(0, balance.getReserved());
        assertEquals(BigDecimal.valueOf(65.50), item.getCostPrice());
        verify(orderItemRepository).save(item);
        verify(ledgerService).post(any());
    }
}
