package com.comunidad360.property.dto;

import com.comunidad360.property.OwnershipType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOwnershipRequest(
        @NotNull UUID userId,
        @NotNull OwnershipType ownershipType,
        @NotNull
        @DecimalMin("0.0001")
        @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 4)
        BigDecimal sharePercentage,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {
}
