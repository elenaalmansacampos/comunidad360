package com.comunidad360.security;

import com.comunidad360.config.AppProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@Profile("prod")
public class ProdSecurityGuard {

    private static final Logger log = LoggerFactory.getLogger(ProdSecurityGuard.class);

    private final AppProperties properties;

    public ProdSecurityGuard(AppProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validate() {
        validateJwtSecret(properties.getSecurity().getJwtSecret());
        if (!properties.getSecurity().isCookieSecure()) {
            throw new IllegalStateException(
                    "app.security.cookie-secure debe ser true en prod (APP_SECURITY_COOKIE_SECURE=true)");
        }
        List<String> origins = properties.getCors().getAllowedOrigins();
        if (origins == null || origins.isEmpty()) {
            throw new IllegalStateException("app.cors.allowed-origins no puede estar vacío en prod");
        }
        for (String origin : origins) {
            if (origin == null || origin.isBlank() || "*".equals(origin)) {
                throw new IllegalStateException("app.cors.allowed-origins contiene un origen no permitido en prod: " + origin);
            }
            String normalized = origin.toLowerCase(Locale.ROOT);
            if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "app.cors.allowed-origins contiene un origen de desarrollo en prod: " + origin);
            }
        }
        log.info("ProdSecurityGuard: configuración de seguridad de producción validada");
    }

    private void validateJwtSecret(String secret) {
        if (secret == null || secret.isBlank()
                || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "app.security.jwt-secret debe tener al menos 32 bytes en prod (APP_SECURITY_JWT_SECRET)");
        }
        String normalized = secret.toLowerCase(Locale.ROOT);
        if (normalized.contains("change-me")
                || normalized.contains("dev-only")
                || normalized.contains("test-only")
                || normalized.contains("cambiar_por")) {
            throw new IllegalStateException(
                    "app.security.jwt-secret parece un secreto de desarrollo; usa uno aleatorio en prod");
        }
    }
}
