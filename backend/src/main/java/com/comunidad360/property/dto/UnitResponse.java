package com.comunidad360.property.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitStatus;
import com.comunidad360.property.UnitType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UnitResponse(
        UUID id,
        UUID buildingId,
        String buildingName,
        String code,
        UnitType type,
        String floor,
        String door,
        BigDecimal builtArea,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal coefficient,
        UnitStatus status,
        Instant archivedAt,
        long version
) {
    public static UnitResponse from(Unit unit, String buildingName) {
        return new UnitResponse(
                unit.getId(),
                unit.getBuildingId(),
                buildingName,
                unit.getCode(),
                unit.getType(),
                unit.getFloor(),
                unit.getDoor(),
                unit.getBuiltArea(),
                unit.getCoefficient(),
                unit.getStatus(),
                unit.getArchivedAt(),
                unit.getVersion()
        );
    }
}
