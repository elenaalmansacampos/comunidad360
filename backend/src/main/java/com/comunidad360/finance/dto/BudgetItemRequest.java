package com.comunidad360.finance.dto;

import com.comunidad360.finance.BudgetItemType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BudgetItemRequest(
        @NotNull BudgetItemType type,
        @NotBlank @Size(max = 100) String category,
        @NotBlank @Size(max = 300) String description,
        @NotNull @DecimalMin("0.0") BigDecimal plannedAmount
) {
}
