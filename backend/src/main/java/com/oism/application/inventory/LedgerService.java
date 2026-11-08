package com.oism.application.inventory;

import com.oism.domain.model.LedgerEntryEntity;
import com.oism.domain.repository.LedgerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    public LedgerService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryEntity> getTenantLedgerHistory(String tenantId, String accountId, int page, int size) {
        return ledgerRepository.findLedgerByTenantAndAccount(tenantId, accountId, PageRequest.of(page, size));
    }

    @Transactional
    public void recordTransaction(String tenantId, String accountId, BigDecimal debit, BigDecimal credit, String desc) {
        BigDecimal debitAmt = debit != null ? debit : BigDecimal.ZERO;
        BigDecimal creditAmt = credit != null ? credit : BigDecimal.ZERO;

        BigDecimal lastBalance = ledgerRepository
                .findTopByTenantIdAndAccountIdOrderByIdDesc(tenantId, accountId)
                .map(LedgerEntryEntity::getBalanceAfter)
                .orElse(BigDecimal.ZERO);

        BigDecimal balanceAfter = lastBalance.add(debitAmt).subtract(creditAmt);

        LedgerEntryEntity entry = LedgerEntryEntity.builder()
                .tenantId(tenantId)
                .accountId(accountId)
                .debitAmount(debitAmt)
                .creditAmount(creditAmt)
                .balanceAfter(balanceAfter)
                .description(desc)
                .createdAt(LocalDateTime.now())
                .build();

        ledgerRepository.save(entry);
    }
}
