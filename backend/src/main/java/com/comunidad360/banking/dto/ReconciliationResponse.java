package com.comunidad360.banking.dto;

import com.comunidad360.banking.Reconciliation;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReconciliationResponse(
        UUID id,
        UUID bankTransactionId,
        UUID paymentId,
        UUID ledgerEntryId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal matchedAmount,
        Instant matchedAt,
        String notes
) {
    public static ReconciliationResponse from(Reconciliation reconciliation) {
        return new ReconciliationResponse(
                reconciliation.getId(),
                reconciliation.getBankTransactionId(),
                reconciliation.getPaymentId(),
                reconciliation.getLedgerEntryId(),
                reconciliation.getMatchedAmount(),
                reconciliation.getMatchedAt(),
                reconciliation.getNotes()
        );
    }
}
