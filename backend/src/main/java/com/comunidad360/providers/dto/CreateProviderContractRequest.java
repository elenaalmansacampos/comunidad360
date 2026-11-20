package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderContractStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProviderContractRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LocalDate startDate,
        LocalDate endDate,
        @DecimalMin("0.00") BigDecimal amount,
        ProviderContractStatus status
) {
}
