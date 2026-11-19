package com.comunidad360.community.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record DashboardMonthResponse(
        int month,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal income,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal expense
) {
}
