package com.oism.application.auth;

import com.oism.domain.model.Role;

import java.util.UUID;

public record UserProfile(UUID userId, UUID tenantId, String tenantName, String email, String phone,
                          String fullName, Role role, UUID branchId) {
}
