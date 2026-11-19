package com.comunidad360.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLanguageRequest(
        @NotBlank String language
) {
}
