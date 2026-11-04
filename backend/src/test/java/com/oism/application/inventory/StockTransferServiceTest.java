package com.oism.application.inventory;

import com.oism.application.inventory.StockTransferService.TransferLine;
import com.oism.application.inventory.StockTransferService.TransferView;
import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Branch;
import com.oism.domain.model.BranchType;
import com.oism.domain.model.CatalogStatus;
import com.oism.domain.model.Sku;
import com.oism.domain.model.StockTransfer;
import com.oism.domain.model.StockTransferItem;
import com.oism.domain.model.TransferStatus;
import com.oism.domain.repository.BranchRepository;
import com.oism.domain.repository.SkuRepository;
import com.oism.domain.repository.StockTransferItemRepository;
import com.oism.domain.repository.StockTransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StockTransferServiceTest {

    @Mock StockTransferRepository transferRepository;
    @Mock StockTransferItemRepository itemRepository;
    @Mock BranchRepository branchRepository;
    @Mock SkuRepository skuRepository;
    @Mock InventoryLedgerService ledgerService;

    StockTransferService service;

    UUID tenantId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID fromBranchId = UUID.randomUUID();
    UUID toBranchId = UUID.randomUUID();
    UUID skuId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        service = new StockTransferService(transferRepository, itemRepository, branchRepository, skuRepository, ledgerService);

        Branch fromBranch = new Branch(tenantId, "Kho tổng", BranchType.WAREHOUSE);
        ReflectionTestUtils.setField(fromBranch, "id", fromBranchId);
        when(branchRepository.findByIdAndTenantId(fromBranchId, tenantId)).thenReturn(Optional.of(fromBranch));

        Branch toBranch = new Branch(tenantId, "Cửa hàng Q1", BranchType.STORE);
        ReflectionTestUtils.setField(toBranch, "id", toBranchId);
        when(branchRepository.findByIdAndTenantId(toBranchId, tenantId)).thenReturn(Optional.of(toBranch));

        Sku sku = new Sku(tenantId, UUID.randomUUID(), "POLO-M", Map.of(), null, BigDecimal.valueOf(100), null, 0);
        ReflectionTestUtils.setField(sku, "id", skuId);
        ReflectionTestUtils.setField(sku, "status", CatalogStatus.ACTIVE);
        sku.setAvgCost(BigDecimal.valueOf(80.00));
        when(skuRepository.findByTenantIdAndIdIn(any(), any())).thenReturn(List.of(sku));
        when(skuRepository.findByIdAndTenantId(skuId, tenantId)).thenReturn(Optional.of(sku));
        when(transferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void create_success_whenValidBranchesAndSkus() {
        TransferView view = service.create(tenantId, userId, fromBranchId, toBranchId, List.of(new TransferLine(skuId, 10)));

        assertEquals(TransferStatus.DRAFT, view.status());
        assertEquals("Kho tổng", view.fromBranchName());
        assertEquals("Cửa hàng Q1", view.toBranchName());
        verify(transferRepository).save(any(StockTransfer.class));
        verify(itemRepository).saveAll(any());
    }

    @Test
    void create_fails_whenSameBranches() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.create(tenantId, userId, fromBranchId, fromBranchId, List.of(new TransferLine(skuId, 5))));

        assertEquals(ErrorCode.IV40001, ex.getErrorCode());
    }

    @Test
    void dispatch_movesToInTransit_andPostsLedgerOut() {
        UUID transferId = UUID.randomUUID();
        StockTransfer transfer = new StockTransfer(tenantId, fromBranchId, toBranchId, "CK-001");
        ReflectionTestUtils.setField(transfer, "id", transferId);
        when(transferRepository.findByIdAndTenantId(transferId, tenantId)).thenReturn(Optional.of(transfer));

        StockTransferItem item = new StockTransferItem(tenantId, transferId, skuId, 10);
        when(itemRepository.findByTenantIdAndTransferId(tenantId, transferId)).thenReturn(List.of(item));

        service.dispatch(tenantId, userId, transferId);

        assertEquals(TransferStatus.IN_TRANSIT, transfer.getStatus());
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerService).post(captor.capture());
        assertEquals(fromBranchId, captor.getValue().branchId());
        assertEquals(10, captor.getValue().quantity());
    }

    @Test
    void receive_movesToReceived_andPostsLedgerIn() {
        UUID transferId = UUID.randomUUID();
        StockTransfer transfer = new StockTransfer(tenantId, fromBranchId, toBranchId, "CK-001");
        ReflectionTestUtils.setField(transfer, "id", transferId);
        transfer.markInTransit();
        when(transferRepository.findByIdAndTenantId(transferId, tenantId)).thenReturn(Optional.of(transfer));

        StockTransferItem item = new StockTransferItem(tenantId, transferId, skuId, 10);
        when(itemRepository.findByTenantIdAndTransferId(tenantId, transferId)).thenReturn(List.of(item));

        service.receive(tenantId, userId, transferId);

        assertEquals(TransferStatus.RECEIVED, transfer.getStatus());
        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerService).post(captor.capture());
        assertEquals(toBranchId, captor.getValue().branchId());
        assertEquals(10, captor.getValue().quantity());
    }
}
