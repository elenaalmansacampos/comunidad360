package com.comunidad360.security;

import com.comunidad360.config.AppProperties;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void createsAndParsesAccessToken() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setJwtSecret("test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        properties.getSecurity().setAccessTokenTtlSeconds(600);
        JwtService jwtService = new JwtService(properties);

        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        AppUser user = new AppUser(
                userId,
                "user@example.com",
                "hash",
                "Test User",
                "es",
                UserStatus.ACTIVE,
                now,
                now
        );

        String token = jwtService.createAccessToken(user);

        assertThat(jwtService.parse(token).getSubject()).isEqualTo(userId.toString());
        assertThat(jwtService.parse(token).get("email", String.class)).isEqualTo("user@example.com");
    }
}
