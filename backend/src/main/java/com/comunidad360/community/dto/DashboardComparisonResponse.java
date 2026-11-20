package com.comunidad360.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record DashboardComparisonResponse(
        int currentMonth,
        int previousMonth,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal currentExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal previousIncome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal previousExpense,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal incomeDelta,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expenseDelta
) {
}
