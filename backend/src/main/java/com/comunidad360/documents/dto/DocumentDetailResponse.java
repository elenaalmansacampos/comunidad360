package com.comunidad360.documents.dto;

import java.util.List;

public record DocumentDetailResponse(
        DocumentResponse document,
        List<DocumentVersionResponse> versions
) {
}
