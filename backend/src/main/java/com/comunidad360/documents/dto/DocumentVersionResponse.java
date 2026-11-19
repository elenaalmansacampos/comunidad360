package com.comunidad360.documents.dto;

import com.comunidad360.documents.DocumentScanStatus;
import com.comunidad360.documents.DocumentVersion;

import java.time.Instant;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        Integer versionNumber,
        String fileName,
        String mimeType,
        Long sizeBytes,
        String uploadedByName,
        Instant uploadedAt,
        DocumentScanStatus scanStatus,
        String downloadUrl
) {
    public static DocumentVersionResponse from(DocumentVersion version, String uploadedByName,
                                               String downloadUrl) {
        return new DocumentVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getFileName(),
                version.getMimeType(),
                version.getSizeBytes(),
                uploadedByName,
                version.getUploadedAt(),
                version.getScanStatus(),
                downloadUrl
        );
    }
}
