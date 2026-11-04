package com.oism.application.auth;

import java.util.UUID;

public record RegisterResult(UUID tenantId, UUID userId, UUID branchId, String email) {
}
