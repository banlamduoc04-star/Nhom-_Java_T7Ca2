package com.oism.domain.repository;

import com.oism.domain.model.DailyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyReportRepository extends JpaRepository<DailyReport, UUID> {

    List<DailyReport> findByTenantIdOrderByReportDateDescBranchIdAscChannelAsc(UUID tenantId);

    /** Chot lai mot ngay: xoa cac dong cu truoc (xoa bang lenh de chay truoc lenh INSERT, tranh trung unique). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from DailyReport d where d.tenantId = :tenantId and d.reportDate = :reportDate")
    int deleteByTenantIdAndReportDate(@Param("tenantId") UUID tenantId, @Param("reportDate") LocalDate reportDate);
}
