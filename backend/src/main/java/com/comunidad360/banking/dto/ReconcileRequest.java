package com.comunidad360.banking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ReconcileRequest(
        UUID paymentId,
        UUID ledgerEntryId,
        @DecimalMin("0.01") BigDecimal matchedAmount,
        @Size(max = 500) String notes
) {
}
