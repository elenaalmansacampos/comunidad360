package com.comunidad360.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record DashboardFinanceResponse(
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
