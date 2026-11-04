package com.oism.application.auth;

public record TokenPair(String accessToken, long accessExpiresInSeconds,
                        String refreshToken, long refreshExpiresInSeconds) {
}
