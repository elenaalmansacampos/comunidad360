package com.comunidad360.property.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTenancyRequest(
        @NotNull UUID userId,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {
}
