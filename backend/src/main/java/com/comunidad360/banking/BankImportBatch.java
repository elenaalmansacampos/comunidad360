package com.comunidad360.banking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_import_batch")
public class BankImportBatch {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "imported_by_user_id")
    private UUID importedByUserId;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "row_count", nullable = false)
    private Integer rowCount = 0;

    @Column(name = "success_count", nullable = false)
    private Integer successCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private Integer duplicateCount = 0;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankImportStatus status;

    protected BankImportBatch() {
    }

    public BankImportBatch(UUID id, UUID communityId, UUID bankAccountId, String fileName, String fileHash,
                           UUID importedByUserId, Instant importedAt, BankImportStatus status) {
        this.id = id;
        this.communityId = communityId;
        this.bankAccountId = bankAccountId;
        this.fileName = fileName;
        this.fileHash = fileHash;
        this.importedByUserId = importedByUserId;
        this.importedAt = importedAt;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getBankAccountId() {
        return bankAccountId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileHash() {
        return fileHash;
    }

    public UUID getImportedByUserId() {
        return importedByUserId;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public BankImportStatus getStatus() {
        return status;
    }

    public void setStatus(BankImportStatus status) {
        this.status = status;
    }
}
