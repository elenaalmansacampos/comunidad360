package com.comunidad360.incidents.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentCommentRequest(
        @NotBlank @Size(max = 5000) String body,
        boolean internal
) {
}
