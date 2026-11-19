package com.comunidad360.finance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.UUID;

public record FinanceSummaryResponse(
        UUID fiscalYearId,
        Integer fiscalYear,
        String scope,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balance,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal income,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expense,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal pendingCharges,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal invoicesPending
) {
}
