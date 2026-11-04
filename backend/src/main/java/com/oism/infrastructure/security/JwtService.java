package com.oism.infrastructure.security;

import com.oism.application.auth.AccessTokenIssuer;
import com.oism.application.auth.AuthenticatedUser;
import com.oism.domain.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Phat hanh va kiem tra access token HS256; claims: sub=userId, tenantId, role, email (60 phut). */
@Component
public class JwtService implements AccessTokenIssuer {

    static final String CLAIM_TENANT = "tenantId";
    static final String CLAIM_ROLE = "role";
    static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(properties.accessTtlMinutes());
    }

    @Override
    public String issue(UUID userId, UUID tenantId, Role role, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(CLAIM_TENANT, tenantId.toString())
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    @Override
    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    /** Tra ve nguoi dung neu token hop le (dung chu ky, chua het han, du claims); nguoc lai rong. */
    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    UUID.fromString(claims.get(CLAIM_TENANT, String.class)),
                    Role.valueOf(claims.get(CLAIM_ROLE, String.class)),
                    claims.get(CLAIM_EMAIL, String.class)));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
