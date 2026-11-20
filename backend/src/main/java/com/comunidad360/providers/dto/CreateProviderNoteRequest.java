package com.comunidad360.providers.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProviderNoteRequest(
        @NotBlank @Size(max = 2000) String body
) {
}
