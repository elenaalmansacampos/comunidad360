package com.comunidad360.property.dto;

import com.comunidad360.property.OwnershipType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateOwnershipRequest(
        OwnershipType ownershipType,
        @DecimalMin("0.0001")
        @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 4)
        BigDecimal sharePercentage,
        LocalDate validFrom,
        LocalDate validTo,
        Long version
) {
}
