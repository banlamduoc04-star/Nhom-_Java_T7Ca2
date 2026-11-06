package com.oism.domain.repository;

import com.oism.domain.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    Optional<DailyReport> findByTenantIdAndReportDate(UUID tenantId, LocalDate reportDate);

    List<DailyReport> findByTenantIdOrderByReportDateDesc(UUID tenantId);
}

