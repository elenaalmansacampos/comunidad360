package com.comunidad360.auth;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
