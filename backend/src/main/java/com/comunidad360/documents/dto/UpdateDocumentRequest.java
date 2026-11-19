package com.comunidad360.documents.dto;

import com.comunidad360.documents.DocumentCategory;
import com.comunidad360.documents.DocumentStatus;
import com.comunidad360.documents.DocumentVisibility;
import jakarta.validation.constraints.Size;

public record UpdateDocumentRequest(
        @Size(max = 300) String title,
        @Size(max = 2000) String description,
        DocumentCategory category,
        DocumentVisibility visibility,
        DocumentStatus status,
        Long version
) {
}
