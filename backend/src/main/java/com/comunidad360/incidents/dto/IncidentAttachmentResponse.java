package com.comunidad360.incidents.dto;

import java.time.Instant;
import java.util.UUID;

public record IncidentAttachmentResponse(
        UUID id,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String uploadedByName,
        Instant createdAt,
        String downloadUrl
) {
}
