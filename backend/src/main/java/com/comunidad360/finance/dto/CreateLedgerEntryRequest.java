package com.comunidad360.finance.dto;

import com.comunidad360.finance.LedgerEntryType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateLedgerEntryRequest(
        @NotNull UUID fiscalYearId,
        @NotNull LocalDate entryDate,
        @NotNull LedgerEntryType type,
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 500) String description,
        @NotNull @DecimalMin("0.01") BigDecimal amount
) {
}
