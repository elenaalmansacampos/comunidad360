package com.comunidad360.incidents.dto;

import com.comunidad360.incidents.IncidentComment;

import java.time.Instant;
import java.util.UUID;

public record IncidentCommentResponse(
        UUID id,
        String body,
        String authorName,
        boolean internal,
        Instant createdAt
) {
    public static IncidentCommentResponse from(IncidentComment comment, String authorName) {
        return new IncidentCommentResponse(
                comment.getId(),
                comment.getBody(),
                authorName,
                comment.isInternal(),
                comment.getCreatedAt()
        );
    }
}
