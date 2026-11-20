package com.oism.application.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Băm SHA-256 dạng hex cho bí mật chỉ lưu bản băm: refresh token, API key webhook (NFR-SEC-01). */
public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 không khả dụng", e);
        }
    }
}
