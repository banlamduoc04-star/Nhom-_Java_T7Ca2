package com.oism.web.auth;

import com.oism.application.auth.UserProfile;

import java.util.UUID;

public record MeResponse(UUID userId, UUID tenantId, String tenantName, String email, String phone,
                         String fullName, String role, UUID branchId) {

    static MeResponse from(UserProfile p) {
        return new MeResponse(p.userId(), p.tenantId(), p.tenantName(), p.email(), p.phone(),
                p.fullName(), p.role().name(), p.branchId());
    }
}
