package com.oism.application.auth;

import com.oism.domain.model.Role;

import java.util.UUID;

/** Nguoi dung da xac thuc, lay tu claims cua access token (tenantId, userId, role). */
public record AuthenticatedUser(UUID userId, UUID tenantId, Role role, String email) {
}
