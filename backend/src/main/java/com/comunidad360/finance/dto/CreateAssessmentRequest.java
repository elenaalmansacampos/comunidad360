package com.comunidad360.finance.dto;

import com.comunidad360.finance.AllocationMethod;
import com.comunidad360.finance.AssessmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateAssessmentRequest(
        @NotNull UUID fiscalYearId,
        @NotBlank @Size(max = 200) String concept,
        @NotNull AssessmentType type,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.01") BigDecimal totalAmount,
        @NotNull AllocationMethod allocationMethod
) {
}
