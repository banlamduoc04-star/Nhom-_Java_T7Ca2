package com.oism.domain.service;

import com.oism.app.infrastructure.entity.LedgerEntryEntity;
import com.oism.app.infrastructure.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service Sổ cái kế toán kép, hỗ trợ phân vùng dữ liệu Đa tenant (NFR-TENANT-02).
 */
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    @Transactional(readOnly = true)
    public List<LedgerEntryEntity> getTenantLedgerHistory(String tenantId, String accountId, int page, int size) {
        return ledgerRepository.findLedgerByTenantAndAccount(tenantId, accountId, PageRequest.of(page, size));
    }

    @Transactional
    public void recordTransaction(String tenantId, String accountId, BigDecimal debit, BigDecimal credit, String desc) {
        BigDecimal debitAmt = debit != null ? debit : BigDecimal.ZERO;
        BigDecimal creditAmt = credit != null ? credit : BigDecimal.ZERO;

        // 1. Lấy số dư ở giao dịch mới nhất (ưu tiên id giảm dần để tránh trùng timestamp)
        BigDecimal lastBalance = ledgerRepository
                .findTopByTenantIdAndAccountIdOrderByIdDesc(tenantId, accountId)
                .map(LedgerEntryEntity::getBalanceAfter)
                .orElse(BigDecimal.ZERO);

        // 2. Tính số dư mới: balanceAfter = lastBalance + debit - credit
        BigDecimal balanceAfter = lastBalance.add(debitAmt).subtract(creditAmt);

        // 3. Tạo bản ghi với số dư vừa tính toán
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