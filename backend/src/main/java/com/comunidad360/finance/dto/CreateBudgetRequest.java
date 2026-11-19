package com.comunidad360.finance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBudgetRequest(
        @NotNull UUID fiscalYearId,
        @Size(max = 2000) String notes
) {
}
