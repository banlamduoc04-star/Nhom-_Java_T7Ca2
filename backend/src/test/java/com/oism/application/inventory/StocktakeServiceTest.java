package com.oism.application.inventory;

import com.oism.application.inventory.StocktakeService.CountInput;
import com.oism.application.inventory.StocktakeService.StocktakeView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockBalance;
import com.oism.domain.model.StockDirection;
import com.oism.domain.model.StockReason;
import com.oism.domain.model.Stocktake;
import com.oism.domain.model.StocktakeItem;
import com.oism.domain.model.StocktakeStatus;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockBalanceRepository;
import com.oism.domain.repository.StocktakeItemRepository;
import com.oism.domain.repository.StocktakeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StocktakeServiceTest {

    @Mock StocktakeRepository stocktakeRepository;
    @Mock StocktakeItemRepository itemRepository;
    @Mock BranchRepository branchRepository;
    @Mock SkuRepository skuRepository;
    @Mock StockBalanceRepository stockBalanceRepository;
    @Mock InventoryLedgerService ledgerService;

    StocktakeService service;

    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID branchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setup() throws Exception {
        service = new StocktakeService(stocktakeRepository, itemRepository, branchRepository,
                skuRepository, stockBalanceRepository, ledgerService);

        Branch branch = new Branch(tenantId, "Chi nhánh 1", BranchType.STORE);
        ReflectionTestUtils.setField(branch, "id", branchId);
        when(branchRepository.findByIdAndTenantId(branchId, tenantId)).thenReturn(Optional.of(branch));

        Sku sku = new Sku(tenantId, UUID.randomUUID(), "TSHIRT-L", Map.of(), null, BigDecimal.valueOf(100), null, 0);
        ReflectionTestUtils.setField(sku, "id", skuId);
        ReflectionTestUtils.setField(sku, "status", CatalogStatus.ACTIVE);
        sku.setAvgCost(BigDecimal.valueOf(50.00));
        when(skuRepository.findByTenantIdAndStatus(tenantId, CatalogStatus.ACTIVE)).thenReturn(List.of(sku));
        when(skuRepository.findByIdAndTenantId(skuId, tenantId)).thenReturn(Optional.of(sku));

        Constructor<StockBalance> c = StockBalance.class.getDeclaredConstructor();
        c.setAccessible(true);
        StockBalance balance = c.newInstance();
        ReflectionTestUtils.setField(balance, "onHand", 20);
        when(stockBalanceRepository.findForUpdate(tenantId, branchId, skuId)).thenReturn(Optional.of(balance));

        when(stocktakeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_capturesSystemQuantityFromOnHand() {
        StocktakeView view = service.create(tenantId, userId, branchId, null);

        assertEquals(StocktakeStatus.OPEN, view.status());
        verify(itemRepository).saveAll(any());
    }

    @Test
    void recordCounts_updatesCountedAndDifference() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(tenantId, branchId, "KK-001");
        ReflectionTestUtils.setField(stocktake, "id", stocktakeId);
        when(stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)).thenReturn(Optional.of(stocktake));

        StocktakeItem item = new StocktakeItem(tenantId, stocktakeId, skuId, 20);
        when(itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId)).thenReturn(List.of(item));

        service.recordCounts(tenantId, userId, stocktakeId, List.of(new CountInput(skuId, 18)));

        assertEquals(18, item.getCountedQty());
        assertEquals(-2, item.getDifference());
        verify(itemRepository).save(item);
    }

    @Test
    void recordCounts_rejectsSkuOutsideSessionWithIV40001() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(tenantId, branchId, "KK-002");
        ReflectionTestUtils.setField(stocktake, "id", stocktakeId);
        when(stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)).thenReturn(Optional.of(stocktake));
        when(itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId))
                .thenReturn(List.of(new StocktakeItem(tenantId, stocktakeId, skuId, 20)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.recordCounts(
                tenantId, userId, stocktakeId, List.of(new CountInput(UUID.randomUUID(), 5))));

        assertEquals(ErrorCode.IV40001, ex.getErrorCode());
    }

    @Test
    void approve_postsAdjustmentsInAscendingSkuOrder() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(tenantId, branchId, "KK-003");
        ReflectionTestUtils.setField(stocktake, "id", stocktakeId);
        when(stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)).thenReturn(Optional.of(stocktake));
        UUID low = new UUID(0, 1);
        UUID high = new UUID(0, 2);
        Sku skuLow = new Sku(tenantId, UUID.randomUUID(), "LOW", Map.of(), null, BigDecimal.TEN, null, 0);
        Sku skuHigh = new Sku(tenantId, UUID.randomUUID(), "HIGH", Map.of(), null, BigDecimal.TEN, null, 0);
        when(skuRepository.findByIdAndTenantId(low, tenantId)).thenReturn(Optional.of(skuLow));
        when(skuRepository.findByIdAndTenantId(high, tenantId)).thenReturn(Optional.of(skuHigh));
        StocktakeItem itemHigh = new StocktakeItem(tenantId, stocktakeId, high, 5);
        itemHigh.updateCountedQty(6);
        StocktakeItem itemLow = new StocktakeItem(tenantId, stocktakeId, low, 5);
        itemLow.updateCountedQty(4);
        when(itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId)).thenReturn(List.of(itemHigh, itemLow));

        service.approve(tenantId, userId, stocktakeId);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerService, times(2)).post(captor.capture());
        assertEquals(List.of(low, high), captor.getAllValues().stream().map(LedgerEntry::skuId).toList());
    }

    @Test
    void approve_createsAdjustLedgerEntry_forVariance() {
        UUID stocktakeId = UUID.randomUUID();
        Stocktake stocktake = new Stocktake(tenantId, branchId, "KK-001");
        ReflectionTestUtils.setField(stocktake, "id", stocktakeId);
        when(stocktakeRepository.findByIdAndTenantId(stocktakeId, tenantId)).thenReturn(Optional.of(stocktake));

        StocktakeItem item = new StocktakeItem(tenantId, stocktakeId, skuId, 20);
        item.updateCountedQty(25); // diff = +5
        when(itemRepository.findByTenantIdAndStocktakeId(tenantId, stocktakeId)).thenReturn(List.of(item));

        service.approve(tenantId, userId, stocktakeId);

        assertEquals(StocktakeStatus.APPROVED, stocktake.getStatus());
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerService).post(captor.capture());
        assertEquals(StockDirection.IN, captor.getValue().direction());
        assertEquals(StockReason.ADJUST, captor.getValue().reason());
        assertEquals(5, captor.getValue().quantity());
    }
}
