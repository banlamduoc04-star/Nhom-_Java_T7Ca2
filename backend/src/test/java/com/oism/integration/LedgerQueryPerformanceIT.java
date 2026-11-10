package com.oism.integration;

import com.oism.domain.model.LedgerEntryEntity;
import com.oism.domain.repository.LedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.StopWatch;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LedgerQueryPerformanceIT extends AbstractIntegrationTest {

    @Autowired
    private LedgerRepository ledgerRepository;

    @Test
    @DisplayName("NFR-PERF-01 & NFR-TENANT-02: Đo hiệu năng truy vấn sổ cái đa tenant trên tập dữ liệu lớn (< 2.0s)")
    void testQueryLedger_1MillionRows_Under2Seconds() {
        String tenantId = "TENANT_PERF_TEST";
        String accountId = "1111";

        StopWatch stopWatch = new StopWatch();
        stopWatch.start("Query Ledger History");

        List<LedgerEntryEntity> results = ledgerRepository.findLedgerByTenantAndAccount(
                tenantId, accountId, PageRequest.of(0, 50));

        stopWatch.stop();

        long elapsedTimeMs = stopWatch.getTotalTimeMillis();
        assertTrue(elapsedTimeMs < 2000, 
                String.format("LỖI HIỆU NĂNG: Truy vấn sổ cái mất %d ms (vượt quá ngưỡng 2000 ms)", elapsedTimeMs));
    }
}
