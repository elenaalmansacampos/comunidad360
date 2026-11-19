package com.comunidad360.auth;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.config.AppProperties;
import com.comunidad360.security.TokenSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final AppProperties properties;

    public RefreshTokenService(RefreshTokenRepository repository, AppProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public IssuedToken create(UUID userId, String userAgent, String ipHash) {
        Instant now = Instant.now();
        String rawToken = TokenSupport.randomToken();
        RefreshToken token = new RefreshToken(
                UUID.randomUUID(),
                userId,
                TokenSupport.sha256Hex(rawToken),
                now.plus(Duration.ofDays(properties.getSecurity().getRefreshTokenTtlDays())),
                userAgent,
                ipHash,
                now
        );
        repository.save(token);
        return new IssuedToken(rawToken, token.getId());
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public RotatedToken rotate(String rawToken, String userAgent, String ipHash) {
        RefreshToken token = repository.findByTokenHash(TokenSupport.sha256Hex(rawToken))
                .orElseThrow(() -> BusinessException.unauthorized(
                        "AUTH_REFRESH_INVALID", "errors.auth.refreshMissing"));

        if (token.getRevokedAt() != null) {
            revokeAll(token.getUserId());
            throw BusinessException.unauthorized("AUTH_REFRESH_REUSED", "errors.auth.refreshReused");
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw BusinessException.unauthorized("AUTH_REFRESH_EXPIRED", "errors.auth.refreshExpired");
        }

        IssuedToken replacement = create(token.getUserId(), userAgent, ipHash);
        token.setRevokedAt(Instant.now());
        token.setReplacedById(replacement.tokenId());
        token.setLastUsedAt(Instant.now());
        repository.save(token);
        return new RotatedToken(replacement.rawToken(), token.getUserId());
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        repository.findByTokenHash(TokenSupport.sha256Hex(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
                repository.save(token);
            }
        });
    }

    @Transactional
    public void revokeAll(UUID userId) {
        List<RefreshToken> tokens = repository.findByUserIdAndRevokedAtIsNull(userId);
        Instant now = Instant.now();
        tokens.forEach(token -> token.setRevokedAt(now));
        repository.saveAll(tokens);
    }

    public record IssuedToken(String rawToken, UUID tokenId) {
    }

    public record RotatedToken(String rawToken, UUID userId) {
    }
}
