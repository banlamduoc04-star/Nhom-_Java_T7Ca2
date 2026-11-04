package com.oism.web.auth;

import com.oism.application.auth.RegisterResult;

import java.util.UUID;

public record RegisterResponse(UUID tenantId, UUID userId, UUID branchId, String email) {

    static RegisterResponse from(RegisterResult r) {
        return new RegisterResponse(r.tenantId(), r.userId(), r.branchId(), r.email());
    }
}
