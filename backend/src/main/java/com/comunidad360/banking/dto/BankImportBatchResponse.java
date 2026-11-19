package com.comunidad360.banking.dto;

import com.comunidad360.banking.BankImportBatch;
import com.comunidad360.banking.BankImportStatus;

import java.time.Instant;
import java.util.UUID;

public record BankImportBatchResponse(
        UUID id,
        UUID bankAccountId,
        String fileName,
        Instant importedAt,
        Integer rowCount,
        Integer successCount,
        Integer duplicateCount,
        Integer errorCount,
        BankImportStatus status
) {
    public static BankImportBatchResponse from(BankImportBatch batch) {
        return new BankImportBatchResponse(
                batch.getId(),
                batch.getBankAccountId(),
                batch.getFileName(),
                batch.getImportedAt(),
                batch.getRowCount(),
                batch.getSuccessCount(),
                batch.getDuplicateCount(),
                batch.getErrorCount(),
                batch.getStatus()
        );
    }
}
