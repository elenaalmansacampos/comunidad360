package com.comunidad360.security;

import com.comunidad360.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProdSecurityGuardTest {

    private static final String STRONG_SECRET = "Zx9Q2mLp7Rt4Vb8Nc1Wd6Yh3Ks0Ja5Gu";

    @Test
    void acceptsHardenedProductionConfiguration() {
        AppProperties properties = properties(STRONG_SECRET, true,
                List.of("https://comunidad360-web.onrender.com"));

        assertThatCode(() -> new ProdSecurityGuard(properties).validate())
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsDevelopmentJwtSecret() {
        AppProperties properties = properties("dev-only-change-me-0123456789-abcdefghijklmnopqrstuvwxyz", true,
                List.of("https://comunidad360-web.onrender.com"));

        assertThatThrownBy(() -> new ProdSecurityGuard(properties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("jwt-secret");
    }

    @Test
    void rejectsShortJwtSecret() {
        AppProperties properties = properties("too-short", true,
                List.of("https://comunidad360-web.onrender.com"));

        assertThatThrownBy(() -> new ProdSecurityGuard(properties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void rejectsInsecureCookie() {
        AppProperties properties = properties(STRONG_SECRET, false,
                List.of("https://comunidad360-web.onrender.com"));

        assertThatThrownBy(() -> new ProdSecurityGuard(properties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cookie-secure");
    }

    @Test
    void rejectsDevelopmentCorsOrigin() {
        AppProperties properties = properties(STRONG_SECRET, true,
                List.of("http://localhost:5173"));

        assertThatThrownBy(() -> new ProdSecurityGuard(properties).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("origen de desarrollo");
    }

    private AppProperties properties(String secret, boolean cookieSecure, List<String> origins) {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setJwtSecret(secret);
        properties.getSecurity().setCookieSecure(cookieSecure);
        properties.getCors().setAllowedOrigins(origins);
        return properties;
    }
}
