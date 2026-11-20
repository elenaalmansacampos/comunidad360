package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderBudgetStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProviderBudgetRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @DecimalMin("0.00") BigDecimal amount,
        LocalDate validUntil,
        ProviderBudgetStatus status,
        Long version
) {
}
