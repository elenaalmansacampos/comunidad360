package com.comunidad360.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record CategoryReportResponse(
        String type,
        String category,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total
) {
}
