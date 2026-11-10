package com.oism.application.inventory;

import com.oism.domain.model.Product;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockAlert;
import com.oism.domain.model.StockBalance;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.ProductRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockAlertRepository;
import com.oism.domain.repository.StockBalanceRepository;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockAlertServiceTest {

    @Mock StockAlertRepository alertRepository;
    @Mock StockBalanceRepository balanceRepository;
    @Mock SkuRepository skuRepository;
    @Mock ProductRepository productRepository;
    @Mock BranchRepository branchRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    StockAlertService service;

    UUID tenantId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();
    UUID prodId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new StockAlertService(
                alertRepository, balanceRepository, skuRepository,
                productRepository, branchRepository, eventPublisher);

        Sku sku = new Sku(tenantId, prodId, "JEAN-BLUE", Map.of(), null,
                BigDecimal.valueOf(300_000), BigDecimal.valueOf(250_000), 5); // reorderPoint = 5
        ReflectionTestUtils.setField(sku, "id", skuId);
        when(skuRepository.findByIdAndTenantId(skuId, tenantId)).thenReturn(Optional.of(sku));
    }

    @Test
    @DisplayName("Sinh cảnh báo OPEN khi available <= reorderPoint và chưa có cảnh báo mở")
    void testTriggerAlertWhenLow() {
        StockBalance balance = new StockBalance(tenantId, branchId, skuId, 4, 1); // available = 3 <= 5
        when(balanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        when(alertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN)).thenReturn(Optional.empty());

        when(alertRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.checkStockLevel(tenantId, branchId, skuId);

        verify(alertRepository).save(any(StockAlert.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Không sinh trùng cảnh báo nếu đã có cảnh báo OPEN")
    void testDoNotDuplicateOpenAlert() {
        StockBalance balance = new StockBalance(tenantId, branchId, skuId, 3, 0); // available = 3 <= 5
        when(balanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        StockAlert existingAlert = new StockAlert(tenantId, branchId, skuId, 5, 3);
        when(alertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN)).thenReturn(Optional.of(existingAlert));

        service.checkStockLevel(tenantId, branchId, skuId);

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tự động chuyển CLOSED khi tồn kho khả dụng vượt ngưỡng đặt lại")
    void testAutoResolveAlertWhenRestocked() {
        StockBalance balance = new StockBalance(tenantId, branchId, skuId, 20, 2); // available = 18 > 5
        when(balanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        StockAlert existingAlert = new StockAlert(tenantId, branchId, skuId, 5, 2);
        when(alertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN)).thenReturn(Optional.of(existingAlert));

        service.checkStockLevel(tenantId, branchId, skuId);

        assertEquals(StockAlert.Status.CLOSED, existingAlert.getStatus());
        assertNotNull(existingAlert.getClosedAt());
        verify(alertRepository).save(existingAlert);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Không theo dõi khi reorder_point = 0: không mở mới, đóng cảnh báo đang mở")
    void zeroThresholdClosesOpenAlertAndNeverOpens() {
        Sku untracked = new Sku(tenantId, prodId, "JEAN-NONE", Map.of(), null,
                BigDecimal.valueOf(300_000), BigDecimal.valueOf(250_000), 0);
        ReflectionTestUtils.setField(untracked, "id", skuId);
        when(skuRepository.findByIdAndTenantId(skuId, tenantId)).thenReturn(Optional.of(untracked));
        when(balanceRepository.findForUpdate(tenantId, branchId, skuId))
                .thenReturn(Optional.of(new StockBalance(tenantId, branchId, skuId, 0, 0)));
        StockAlert existingAlert = new StockAlert(tenantId, branchId, skuId, 5, 1);
        when(alertRepository.findByTenantIdAndBranchIdAndSkuIdAndStatus(
                tenantId, branchId, skuId, StockAlert.Status.OPEN)).thenReturn(Optional.of(existingAlert));

        service.checkStockLevel(tenantId, branchId, skuId);

        assertEquals(StockAlert.Status.CLOSED, existingAlert.getStatus());
        verify(alertRepository).save(existingAlert);
    }

    @Test
    @DisplayName("Đóng cảnh báo của tenant khác -> 404 IV40401")
    void closeAlertOfOtherTenantIsNotFound() {
        UUID alertId = UUID.randomUUID();
        when(alertRepository.findByIdAndTenantId(alertId, tenantId)).thenReturn(Optional.empty());

        com.oism.domain.exception.BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                com.oism.domain.exception.BusinessException.class, () -> service.closeAlert(tenantId, alertId));

        assertEquals(com.oism.domain.exception.ErrorCode.IV40401, ex.getErrorCode());
    }
}
