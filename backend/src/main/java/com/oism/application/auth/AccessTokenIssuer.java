package com.oism.application.auth;

import com.oism.domain.model.Role;

import java.util.UUID;

/** Cong (port) phat hanh access token; infrastructure.security.JwtService hien thuc. */
public interface AccessTokenIssuer {

    String issue(UUID userId, UUID tenantId, Role role, String email);

    /** Thoi han access token, tinh bang giay. */
    long ttlSeconds();
}
