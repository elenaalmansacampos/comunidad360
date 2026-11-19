package com.comunidad360.finance;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.security.TokenSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public Optional<UUID> findExisting(UUID communityId, UUID userId, String operation,
                                       String idempotencyKey, Object request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        String hash = hash(request);
        return repository.findByCommunityIdAndUserIdAndOperationAndIdempotencyKey(
                        communityId, userId, operation, idempotencyKey)
                .map(record -> {
                    if (!record.getRequestHash().equals(hash)) {
                        throw BusinessException.conflict(
                                "IDEMPOTENCY_CONFLICT", "errors.idempotency.conflict");
                    }
                    return record.getResourceId();
                });
    }

    public void record(UUID communityId, UUID userId, String operation, String idempotencyKey,
                       Object request, UUID resourceId) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return;
        }
        repository.save(new IdempotencyRecord(
                UUID.randomUUID(),
                communityId,
                userId,
                operation,
                idempotencyKey,
                hash(request),
                resourceId,
                Instant.now()
        ));
    }

    private String hash(Object request) {
        try {
            String json = request == null ? "null" : objectMapper.writeValueAsString(request);
            return TokenSupport.sha256Hex(json);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot hash idempotency request", exception);
        }
    }
}
