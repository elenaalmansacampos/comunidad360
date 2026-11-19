package com.comunidad360.property.dto;

import com.comunidad360.property.TenancyStatus;

import java.time.LocalDate;
import java.util.UUID;

public record TenancyResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        LocalDate validFrom,
        LocalDate validTo,
        TenancyStatus status,
        boolean active,
        long version
) {
}
