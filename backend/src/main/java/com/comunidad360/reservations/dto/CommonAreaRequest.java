package com.comunidad360.reservations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CommonAreaRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @Size(max = 200) String location,
        @PositiveOrZero Integer capacity,
        Boolean requiresApproval,
        Boolean active
) {
}
