package com.oism.web.auth;

import com.oism.application.auth.TokenPair;

public record TokenResponse(String accessToken, String refreshToken, String tokenType,
                            long accessExpiresIn, long refreshExpiresIn) {

    static TokenResponse from(TokenPair pair) {
        return new TokenResponse(pair.accessToken(), pair.refreshToken(), "Bearer",
                pair.accessExpiresInSeconds(), pair.refreshExpiresInSeconds());
    }
}
