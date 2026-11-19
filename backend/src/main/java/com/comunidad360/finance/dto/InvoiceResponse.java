package com.comunidad360.finance.dto;

import com.comunidad360.finance.Invoice;
import com.comunidad360.finance.InvoiceStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID fiscalYearId,
        String providerName,
        String number,
        LocalDate issueDate,
        LocalDate dueDate,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal subtotal,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal taxAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal total,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal paidAmount,
        String currency,
        InvoiceStatus status,
        long version
) {
    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getFiscalYearId(),
                invoice.getProviderName(),
                invoice.getNumber(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotal(),
                invoice.getPaidAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getVersion()
        );
    }
}
