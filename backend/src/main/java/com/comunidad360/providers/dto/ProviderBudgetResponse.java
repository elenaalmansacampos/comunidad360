package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderBudget;
import com.comunidad360.providers.ProviderBudgetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProviderBudgetResponse(
        UUID id, String title, String description, BigDecimal amount, LocalDate validUntil,
        ProviderBudgetStatus status, Instant createdAt, Instant updatedAt, long version
) {
    public static ProviderBudgetResponse from(ProviderBudget budget) {
        return new ProviderBudgetResponse(budget.getId(), budget.getTitle(), budget.getDescription(),
                budget.getAmount(), budget.getValidUntil(), budget.getStatus(),
                budget.getCreatedAt(), budget.getUpdatedAt(), budget.getVersion());
    }
}
