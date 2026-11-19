package com.comunidad360.finance.dto;

import com.comunidad360.finance.Payment;
import com.comunidad360.finance.PaymentMethod;
import com.comunidad360.finance.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID unitId,
        UUID assessmentChargeId,
        UUID invoiceId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        String reference,
        PaymentStatus status,
        long version
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getUnitId(),
                payment.getAssessmentChargeId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getMethod(),
                payment.getReference(),
                payment.getStatus(),
                payment.getVersion()
        );
    }
}
