package com.comunidad360.property.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record StructureSummaryResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal coefficientSum,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal tolerance,
        boolean valid,
        long totalUnits,
        long activeUnits,
        long buildings,
        long commonElements
) {
}
