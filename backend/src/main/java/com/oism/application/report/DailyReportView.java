package com.oism.application.report;

import com.oism.domain.model.OrderChannel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Mot dong bao cao chot ngay: chi nhanh x ngay x kenh (FR-SIM-03, bang daily_report). */
public record DailyReportView(
        UUID id,
        LocalDate reportDate,
        UUID branchId,
        String branchName,
        OrderChannel channel,
        int orderCount,
        BigDecimal revenue,
        BigDecimal cogs,
        BigDecimal grossProfit,
        Instant createdAt) {
}
