package com.oism.infrastructure.scheduler;

import com.oism.application.report.DailyReportService;
import com.oism.domain.model.Tenant;
import com.oism.domain.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Tac vu nen (FR-SIM-03, JC-31):
 * Chay moi dem luc 02:00 UTC de chot so bao cao doanh thu, gia von va loi nhuan ngay hom truoc vao daily_report.
 */
@Component
public class DailyReportJob {

    private static final Logger log = LoggerFactory.getLogger(DailyReportJob.class);

    private final TenantRepository tenantRepository;
    private final DailyReportService dailyReportService;

    public DailyReportJob(TenantRepository tenantRepository, DailyReportService dailyReportService) {
        this.tenantRepository = tenantRepository;
        this.dailyReportService = dailyReportService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyReport() {
        LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            if (!tenant.isActive()) {
                continue;
            }
            try {
                dailyReportService.generateDailyReport(tenant.getId(), yesterday);
                log.info("Chot daily report ngay {} cho tenant {} thanh cong", yesterday, tenant.getName());
            } catch (Exception e) {
                log.error("Loi khi tao daily report cho tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }
}

