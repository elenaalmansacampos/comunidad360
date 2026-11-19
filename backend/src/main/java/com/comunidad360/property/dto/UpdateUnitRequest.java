package com.comunidad360.property.dto;

import com.comunidad360.property.UnitType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateUnitRequest(
        UUID buildingId,
        @Size(max = 50) String code,
        UnitType type,
        @Size(max = 20) String floor,
        @Size(max = 20) String door,
        @DecimalMin("0.0") BigDecimal builtArea,
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 6)
        BigDecimal coefficient,
        Long version
) {
}
