package com.comunidad360.property.dto;

import com.comunidad360.property.OwnershipType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OwnershipResponse(
        UUID id,
        UUID userId,
        String fullName,
        String email,
        OwnershipType ownershipType,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal sharePercentage,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active,
        long version
) {
}
