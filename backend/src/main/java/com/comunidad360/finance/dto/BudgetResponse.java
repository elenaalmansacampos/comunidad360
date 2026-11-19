package com.comunidad360.finance.dto;

import com.comunidad360.finance.Budget;
import com.comunidad360.finance.BudgetStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID fiscalYearId,
        BudgetStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalExpense,
        String notes,
        Instant approvedAt,
        List<BudgetItemResponse> items,
        long version
) {
    public static BudgetResponse from(Budget budget, List<BudgetItemResponse> items) {
        return new BudgetResponse(
                budget.getId(),
                budget.getFiscalYearId(),
                budget.getStatus(),
                budget.getTotalIncome(),
                budget.getTotalExpense(),
                budget.getNotes(),
                budget.getApprovedAt(),
                items,
                budget.getVersion()
        );
    }
}
