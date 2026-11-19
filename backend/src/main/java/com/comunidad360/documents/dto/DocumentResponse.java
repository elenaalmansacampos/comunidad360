package com.comunidad360.documents.dto;

import com.comunidad360.documents.Document;
import com.comunidad360.documents.DocumentCategory;
import com.comunidad360.documents.DocumentStatus;
import com.comunidad360.documents.DocumentVisibility;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        DocumentCategory category,
        String title,
        String description,
        DocumentVisibility visibility,
        DocumentStatus status,
        UUID ownerUserId,
        Integer currentVersionNumber,
        String fileName,
        String mimeType,
        Long sizeBytes,
        Instant createdAt,
        Instant updatedAt,
        String downloadUrl,
        long version
) {
    public static DocumentResponse from(Document document, Integer currentVersionNumber,
                                        String fileName, String mimeType, Long sizeBytes,
                                        String downloadUrl) {
        return new DocumentResponse(
                document.getId(),
                document.getCategory(),
                document.getTitle(),
                document.getDescription(),
                document.getVisibility(),
                document.getStatus(),
                document.getOwnerUserId(),
                currentVersionNumber,
                fileName,
                mimeType,
                sizeBytes,
                document.getCreatedAt(),
                document.getUpdatedAt(),
                downloadUrl,
                document.getVersion()
        );
    }
}
