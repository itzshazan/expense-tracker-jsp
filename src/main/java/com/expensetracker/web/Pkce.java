package com.expensetracker.web;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

record Pkce(String verifier, Instant expiresAt) {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_BASE64 = Base64.getUrlEncoder().withoutPadding();

    static Pkce create(Duration lifetime) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new Pkce(URL_BASE64.encodeToString(bytes), Instant.now().plus(lifetime));
    }

    boolean expired() {
        return !Instant.now().isBefore(expiresAt);
    }

    String challenge() {
        try {
            return URL_BASE64.encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
