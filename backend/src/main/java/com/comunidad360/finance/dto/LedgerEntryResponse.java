package com.comunidad360.finance.dto;

import com.comunidad360.finance.LedgerEntry;
import com.comunidad360.finance.LedgerEntryStatus;
import com.comunidad360.finance.LedgerEntryType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID id,
        UUID fiscalYearId,
        Long entryNumber,
        LocalDate entryDate,
        LedgerEntryType type,
        String category,
        String description,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal amount,
        LedgerEntryStatus status,
        UUID reversalOfId,
        Instant postedAt,
        long version
) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getFiscalYearId(),
                entry.getEntryNumber(),
                entry.getEntryDate(),
                entry.getType(),
                entry.getCategory(),
                entry.getDescription(),
                entry.getAmount(),
                entry.getStatus(),
                entry.getReversalOfId(),
                entry.getPostedAt(),
                entry.getVersion()
        );
    }
}
