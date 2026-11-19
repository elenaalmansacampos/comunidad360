package com.comunidad360.meetings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AgendaItemRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        Integer position
) {
}
