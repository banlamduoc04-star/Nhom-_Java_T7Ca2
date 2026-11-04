package com.oism.infrastructure.scheduler;

import com.oism.application.order.OrderService;
import com.oism.domain.model.Tenant;
import com.oism.domain.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tac vu nen (FR-SIM-03, JC-31):
 * Quet moi phut mot lan de tu dong huy cac don hang o trang thai RESERVED da qua han reserved_until.
 */
@Component
public class ReservationExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirationJob.class);

    private final TenantRepository tenantRepository;
    private final OrderService orderService;

    public ReservationExpirationJob(TenantRepository tenantRepository, OrderService orderService) {
        this.tenantRepository = tenantRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    public void cancelExpiredReservations() {
        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant tenant : tenants) {
            if (!tenant.isActive()) {
                continue;
            }
            try {
                int cancelledCount = orderService.cancelExpiredReservations(tenant.getId());
                if (cancelledCount > 0) {
                    log.info("Tenant {}: da tu dong huy {} don hang qua han giu hang",
                            tenant.getName(), cancelledCount);
                }
            } catch (Exception e) {
                log.error("Loi khi quet don qua han cua tenant {}: {}", tenant.getId(), e.getMessage());
            }
        }
    }
}
