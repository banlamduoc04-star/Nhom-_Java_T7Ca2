package com.oism.application.order;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.AppUser;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.OrderStatus;
import com.oism.domain.model.PaymentMethod;
import com.oism.domain.model.Product;
import com.oism.domain.model.Role;
import com.oism.domain.model.SalesOrder;
import com.oism.domain.model.Sku;
import com.oism.domain.repository.AppUserRepository;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.OrderItemRepository;
import com.oism.domain.repository.OrderStatusHistoryRepository;
import com.oism.domain.repository.PaymentRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SalesOrderRepository;
import com.oism.domain.repository.SkuRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PosCheckoutServiceTest {

    @Mock SalesOrderRepository orderRepository;
    @Mock OrderItemRepository itemRepository;
    @Mock OrderStatusHistoryRepository historyRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock BranchRepository branchRepository;
    @Mock AppUserRepository appUserRepository;
    @Mock SkuRepository skuRepository;
    @Mock ProductRepository productRepository;
    @Mock ReservationService reservationService;
    @Mock ApplicationEventPublisher eventPublisher;

    PosCheckoutService service;

    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();
    UUID prodId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new PosCheckoutService(
                orderRepository, itemRepository, historyRepository, paymentRepository,
                branchRepository, appUserRepository, skuRepository, productRepository,
                reservationService, eventPublisher);

        Branch branch = new Branch(tenantId, "Chi nhánh POS", BranchType.STORE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByIdAndTenantId(branchId, tenantId)).thenReturn(Optional.of(branch));
        when(appUserRepository.findByIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(user(Role.CASHIER, branchId)));

        Product product = new Product(tenantId, null, null, "Áo sơ mi nam", null, false);
        ReflectionTestUtils.setField(product, "id", prodId);
        when(productRepository.findAllById(any())).thenReturn(List.of(product));

        Sku sku = new Sku(tenantId, prodId, "SHIRT-M", Map.of("Size", "M"), "8931234567890",
                BigDecimal.valueOf(200_000), BigDecimal.valueOf(180_000), 5);
        ReflectionTestUtils.setField(sku, "id", skuId);
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));

        when(orderRepository.save(any())).thenAnswer(inv -> {
            SalesOrder o = inv.getArgument(0);
            ReflectionTestUtils.setField(o, "id", UUID.randomUUID());
            return o;
        });
    }

    @Test
    @DisplayName("Thanh toán tiền mặt: tính tổng tiền và tiền thối chính xác, trạng thái COMPLETED")
    void testCheckoutCashSuccess() {
        PosCheckoutCommand cmd = new PosCheckoutCommand(
                branchId,
                List.of(new PosCheckoutCommand.PosLine(skuId, 2)),
                PaymentMethod.CASH,
                BigDecimal.valueOf(500_000),
                "Nguyễn Văn A",
                "0987654321",
                "POS Order"
        );

        PosCheckoutResponse resp = service.checkout(tenantId, userId, "Thu ngân 1", cmd);

        assertNotNull(resp);
        assertEquals(BigDecimal.valueOf(400_000), resp.totalAmount());
        assertEquals(BigDecimal.valueOf(500_000), resp.cashReceived());
        assertEquals(BigDecimal.valueOf(100_000), resp.changeAmount());
        assertEquals(OrderStatus.COMPLETED, resp.status());
        assertEquals(PaymentMethod.CASH, resp.paymentMethod());

        verify(reservationService).reserve(any(), any(), any());
        verify(reservationService).confirm(any(), any(), any(), any(), any());
        verify(paymentRepository).save(any());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Thanh toán thất bại khi không đủ tồn kho khả dụng (ném IV40901)")
    void testCheckoutInsufficientStock() {
        doThrow(new BusinessException(ErrorCode.IV40901, "Không đủ hàng khả dụng"))
                .when(reservationService).reserve(any(), any(), any());

        PosCheckoutCommand cmd = new PosCheckoutCommand(
                branchId,
                List.of(new PosCheckoutCommand.PosLine(skuId, 10)),
                PaymentMethod.CASH,
                BigDecimal.valueOf(2_000_000),
                null, null, null
        );

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.checkout(tenantId, userId, "Thu ngân 1", cmd));
        assertEquals(ErrorCode.IV40901, ex.getErrorCode());
    }

    @Test
    @DisplayName("Thanh toán qua QR: tiền khách đưa đúng bằng tổng tiền, tiền thối bằng 0")
    void testCheckoutQrSuccess() {
        PosCheckoutCommand cmd = new PosCheckoutCommand(
                branchId,
                List.of(new PosCheckoutCommand.PosLine(skuId, 1)),
                PaymentMethod.QR,
                null,
                null, null, null
        );

        PosCheckoutResponse resp = service.checkout(tenantId, userId, "Thu ngân 1", cmd);

        assertEquals(BigDecimal.valueOf(200_000), resp.totalAmount());
        assertEquals(BigDecimal.valueOf(200_000), resp.cashReceived());
        assertEquals(BigDecimal.ZERO, resp.changeAmount());
        assertEquals(PaymentMethod.QR, resp.paymentMethod());
    }

    @Test
    @DisplayName("FR-AUTH-05: Cashier bán ở chi nhánh khác chi nhánh được gán -> 403 AU40301, không tạo đơn")
    void cashierCannotSellAtOtherBranch() {
        when(appUserRepository.findByIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(user(Role.CASHIER, UUID.randomUUID())));
        PosCheckoutCommand cmd = new PosCheckoutCommand(branchId, List.of(new PosCheckoutCommand.PosLine(skuId, 1)),
                PaymentMethod.CASH, BigDecimal.valueOf(500_000), null, null, null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.checkout(tenantId, userId, "Thu ngân 1", cmd));

        assertEquals(ErrorCode.AU40301, ex.getErrorCode());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("FR-AUTH-05: Owner bán được ở mọi chi nhánh của cửa hàng")
    void ownerCanSellAtAnyBranch() {
        when(appUserRepository.findByIdAndTenantId(userId, tenantId))
                .thenReturn(Optional.of(user(Role.OWNER, UUID.randomUUID())));
        PosCheckoutCommand cmd = new PosCheckoutCommand(branchId, List.of(new PosCheckoutCommand.PosLine(skuId, 1)),
                PaymentMethod.CASH, BigDecimal.valueOf(500_000), null, null, null);

        PosCheckoutResponse resp = service.checkout(tenantId, userId, "Chủ cửa hàng", cmd);

        assertEquals(OrderStatus.COMPLETED, resp.status());
    }

    private AppUser user(Role role, UUID assignedBranch) {
        return new AppUser(tenantId, "nv@oism.vn", null, "hash", "Nhân viên", role, assignedBranch);
    }
}
