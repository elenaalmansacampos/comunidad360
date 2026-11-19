package com.comunidad360.finance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateFiscalYearRequest(
        @NotNull @Min(2000) @Max(2100) Integer year,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
