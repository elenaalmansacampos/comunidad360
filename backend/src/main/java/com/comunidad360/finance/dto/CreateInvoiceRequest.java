package com.comunidad360.finance.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID fiscalYearId,
        @NotBlank @Size(max = 200) String providerName,
        @NotBlank @Size(max = 100) String number,
        @NotNull LocalDate issueDate,
        LocalDate dueDate,
        @NotNull @DecimalMin("0.0") BigDecimal subtotal,
        @NotNull @DecimalMin("0.0") BigDecimal taxAmount,
        @NotNull @DecimalMin("0.0") BigDecimal total
) {
}
