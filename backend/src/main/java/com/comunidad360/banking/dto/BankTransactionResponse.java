package com.comunidad360.banking.dto;

import com.comunidad360.banking.BankTransaction;
import com.comunidad360.banking.BankTransactionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BankTransactionResponse(
        UUID id,
        UUID bankAccountId,
        LocalDate valueDate,
        LocalDate bookingDate,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal balanceAfter,
        String currency,
        BankTransactionStatus status,
        UUID reconciliationId,
        UUID paymentId,
        UUID ledgerEntryId,
        long version
) {
    public static BankTransactionResponse from(BankTransaction transaction, UUID reconciliationId,
                                               UUID paymentId, UUID ledgerEntryId) {
        return new BankTransactionResponse(
                transaction.getId(),
                transaction.getBankAccountId(),
                transaction.getValueDate(),
                transaction.getBookingDate(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getBalanceAfter(),
                transaction.getCurrency(),
                transaction.getStatus(),
                reconciliationId,
                paymentId,
                ledgerEntryId,
                transaction.getVersion()
        );
    }
}
