package com.oism.infrastructure.security;

import com.oism.application.auth.AuthenticatedUser;
import com.oism.domain.model.Role;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghij";

    private final JwtService service = new JwtService(new JwtProperties(SECRET, 60));

    @Test
    void issuedTokenCarriesTenantUserAndRole() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = service.issue(userId, tenantId, Role.OWNER, "a@b.vn");
        Optional<AuthenticatedUser> parsed = service.parse(token);

        assertTrue(parsed.isPresent());
        assertEquals(userId, parsed.get().userId());
        assertEquals(tenantId, parsed.get().tenantId());
        assertEquals(Role.OWNER, parsed.get().role());
        assertEquals(3600, service.ttlSeconds());
    }

    @Test
    void tamperedOrForeignTokenIsRejected() {
        String token = service.issue(UUID.randomUUID(), UUID.randomUUID(), Role.STAFF, "a@b.vn");
        JwtService other = new JwtService(new JwtProperties("another-secret-0123456789-abcdefghijkl", 60));

        assertTrue(service.parse(token + "x").isEmpty());
        assertTrue(other.parse(token).isEmpty());
        assertTrue(service.parse("not-a-jwt").isEmpty());
    }
}
