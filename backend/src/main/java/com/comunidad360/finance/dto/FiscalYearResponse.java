package com.comunidad360.finance.dto;

import com.comunidad360.finance.FiscalYear;
import com.comunidad360.finance.FiscalYearStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FiscalYearResponse(
        UUID id,
        Integer year,
        LocalDate startDate,
        LocalDate endDate,
        FiscalYearStatus status,
        Instant closedAt,
        long version
) {
    public static FiscalYearResponse from(FiscalYear fiscalYear) {
        return new FiscalYearResponse(
                fiscalYear.getId(),
                fiscalYear.getYear(),
                fiscalYear.getStartDate(),
                fiscalYear.getEndDate(),
                fiscalYear.getStatus(),
                fiscalYear.getClosedAt(),
                fiscalYear.getVersion()
        );
    }
}
