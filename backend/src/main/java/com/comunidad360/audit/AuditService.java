package com.comunidad360.audit;

import com.comunidad360.common.api.TraceIdFilter;
import com.comunidad360.security.RequestInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID communityId, UUID actorUserId, String actorType, String action,
                       String entityType, UUID entityId, Object before, Object after,
                       HttpServletRequest request) {
        AuditLog auditLog = new AuditLog(
                UUID.randomUUID(),
                communityId,
                actorUserId,
                actorType,
                action,
                entityType,
                entityId,
                toJson(before),
                toJson(after),
                request == null ? null : RequestInfo.ipHash(request),
                request == null ? null : RequestInfo.userAgent(request),
                MDC.get(TraceIdFilter.TRACE_ID),
                Instant.now()
        );
        repository.save(auditLog);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return null;
        }
    }
}
