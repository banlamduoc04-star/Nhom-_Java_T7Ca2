package com.oism.application.tenant;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import com.oism.domain.model.Tenant;
import com.oism.domain.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantSettingsService {

    private final TenantRepository tenantRepository;

    public TenantSettingsService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public record TenantSettingsView(UUID tenantId, String tenantName, int reservationMinutes) {}

    @Transactional(readOnly = true)
    public TenantSettingsView getSettings(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy tenant"));
        return new TenantSettingsView(tenant.getId(), tenant.getName(), tenant.getReservationMinutes());
    }

    @Transactional
    public TenantSettingsView updateSettings(UUID tenantId, int reservationMinutes) {
        if (reservationMinutes < 1 || reservationMinutes > 1440) {
            throw new BusinessException(ErrorCode.OD40001, "Thời hạn giữ hàng phải từ 1 đến 1440 phút (24 giờ)");
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AU40401, "Không tìm thấy tenant"));
        tenant.setReservationMinutes(reservationMinutes);
        tenantRepository.save(tenant);
        return new TenantSettingsView(tenant.getId(), tenant.getName(), tenant.getReservationMinutes());
    }
}

