package com.oism.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cau hinh JWT (oism.jwt.*). secret toi thieu 32 ky tu (HS256); khong commit gia tri that,
 * doc tu bien moi truong OISM_JWT_SECRET (NFR-SEC-01).
 */
@ConfigurationProperties(prefix = "oism.jwt")
public record JwtProperties(String secret, long accessTtlMinutes) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("oism.jwt.secret phải có ít nhất 32 byte (đặt biến môi trường OISM_JWT_SECRET)");
        }
        if (accessTtlMinutes <= 0) {
            accessTtlMinutes = 60;
        }
    }
}
