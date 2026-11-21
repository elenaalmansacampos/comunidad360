package com.comunidad360.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatSocketMessage(
        @NotBlank @Size(max = 4000) String body
) {
}
