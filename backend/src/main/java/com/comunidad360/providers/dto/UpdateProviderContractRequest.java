package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderContractStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateProviderContractRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate endDate,
        @DecimalMin("0.00") BigDecimal amount,
        ProviderContractStatus status,
        Long version
) {
}
