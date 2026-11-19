package com.comunidad360.finance.dto;

import com.comunidad360.finance.AllocationMethod;
import com.comunidad360.finance.Assessment;
import com.comunidad360.finance.AssessmentStatus;
import com.comunidad360.finance.AssessmentType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AssessmentResponse(
        UUID id,
        UUID fiscalYearId,
        String concept,
        AssessmentType type,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate dueDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAmount,
        AllocationMethod allocationMethod,
        AssessmentStatus status,
        long chargesCount,
        Instant issuedAt,
        long version
) {
    public static AssessmentResponse from(Assessment assessment, long chargesCount) {
        return new AssessmentResponse(
                assessment.getId(),
                assessment.getFiscalYearId(),
                assessment.getConcept(),
                assessment.getType(),
                assessment.getPeriodStart(),
                assessment.getPeriodEnd(),
                assessment.getDueDate(),
                assessment.getTotalAmount(),
                assessment.getAllocationMethod(),
                assessment.getStatus(),
                chargesCount,
                assessment.getIssuedAt(),
                assessment.getVersion()
        );
    }
}
