package com.comunidad360.property.dto;

import com.comunidad360.property.UnitType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateUnitRequest(
        UUID buildingId,
        @NotBlank @Size(max = 50) String code,
        @NotNull UnitType type,
        @Size(max = 20) String floor,
        @Size(max = 20) String door,
        @DecimalMin("0.0") BigDecimal builtArea,
        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        @Digits(integer = 3, fraction = 6)
        BigDecimal coefficient
) {
}
