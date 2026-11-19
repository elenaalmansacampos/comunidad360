package com.comunidad360.finance.dto;

import com.comunidad360.finance.AssessmentCharge;
import com.comunidad360.finance.ChargeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AssessmentChargeResponse(
        UUID id,
        UUID assessmentId,
        UUID unitId,
        String unitCode,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal coefficientSnapshot,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paidAmount,
        ChargeStatus status,
        LocalDate dueDate,
        long version
) {
    public static AssessmentChargeResponse from(AssessmentCharge charge, String unitCode, LocalDate dueDate) {
        return new AssessmentChargeResponse(
                charge.getId(),
                charge.getAssessmentId(),
                charge.getUnitId(),
                unitCode,
                charge.getAmount(),
                charge.getCoefficientSnapshot(),
                charge.getPaidAmount(),
                charge.getStatus(),
                dueDate,
                charge.getVersion()
        );
    }
}
