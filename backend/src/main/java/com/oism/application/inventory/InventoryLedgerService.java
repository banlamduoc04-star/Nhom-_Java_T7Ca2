package com.oism.application.inventory;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.InventoryTransaction;
import com.oism.domain.model.StockBalance;
import com.oism.domain.model.StockDirection;
import com.oism.domain.repository.InventoryTransactionRepository;
import com.oism.domain.repository.StockBalanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cua duy nhat de doi ton kho (Ke hoach muc 4 - "So cai ton kho", FR-INV-01): khoa dong stock_balance,
 * cap nhat on_hand va ghi mot dong inventory_transaction (balance_after = on_hand moi) trong cung giao dich.
 * Khong noi nao khac duoc sua on_hand. MANDATORY: bat buoc co giao dich cua nguoi goi de khoa co tac dung.
 */
@Service
public class InventoryLedgerService {

    private final StockBalanceRepository stockBalanceRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final StockAlertService stockAlertService;

    public InventoryLedgerService(StockBalanceRepository stockBalanceRepository,
                                  InventoryTransactionRepository inventoryTransactionRepository,
                                  StockAlertService stockAlertService) {
        this.stockBalanceRepository = stockBalanceRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.stockAlertService = stockAlertService;
    }

    /** Ghi but toan va tra ve on_hand sau khi ghi. OUT vuot hang kha dung -> IV40901. */
    @Transactional(propagation = Propagation.MANDATORY)
    public int post(LedgerEntry entry) {
        if (entry.quantity() <= 0) {
            throw new BusinessException(ErrorCode.IV40001, "Số lượng phải lớn hơn 0");
        }
        stockBalanceRepository.insertIfAbsent(entry.tenantId(), entry.branchId(), entry.skuId());
        StockBalance balance = stockBalanceRepository
                .findForUpdate(entry.tenantId(), entry.branchId(), entry.skuId())
                .orElseThrow(() -> new IllegalStateException("Không khóa được dòng tồn kho"));

        int newOnHand;
        if (entry.direction() == StockDirection.IN) {
            newOnHand = balance.getOnHand() + entry.quantity();
        } else {
            if (balance.getAvailable() < entry.quantity()) {
                throw new BusinessException(ErrorCode.IV40901,
                        "Không đủ hàng khả dụng (còn " + balance.getAvailable() + ")");
            }
            newOnHand = balance.getOnHand() - entry.quantity();
        }

        balance.applyOnHand(newOnHand);
        stockBalanceRepository.save(balance);
        inventoryTransactionRepository.save(new InventoryTransaction(
                entry.tenantId(), entry.branchId(), entry.skuId(), entry.direction(), entry.reason(),
                entry.quantity(), newOnHand, entry.unitCost(), entry.refType(), entry.refId(), entry.createdBy()));
        // FR-REP-03: on_hand doi thi available doi -> mo/dong canh bao ton thap trong cung giao dich
        stockAlertService.checkStockLevel(entry.tenantId(), entry.branchId(), entry.skuId());
        return newOnHand;
    }
}
