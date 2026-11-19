package com.comunidad360.banking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "bank_transaction")
public class BankTransaction {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "bank_account_id", nullable = false)
    private UUID bankAccountId;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "external_id", length = 200)
    private String externalId;

    @Column(name = "value_date", nullable = false)
    private LocalDate valueDate;

    @Column(name = "booking_date")
    private LocalDate bookingDate;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "raw_hash", nullable = false, length = 64)
    private String rawHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankTransactionStatus status;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BankTransaction() {
    }

    public BankTransaction(UUID id, UUID communityId, UUID bankAccountId, UUID importBatchId,
                           String externalId, LocalDate valueDate, LocalDate bookingDate, String description,
                           BigDecimal amount, BigDecimal balanceAfter, String rawHash, Instant importedAt) {
        this.id = id;
        this.communityId = communityId;
        this.bankAccountId = bankAccountId;
        this.importBatchId = importBatchId;
        this.externalId = externalId;
        this.valueDate = valueDate;
        this.bookingDate = bookingDate;
        this.description = description;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.rawHash = rawHash;
        this.status = BankTransactionStatus.UNRECONCILED;
        this.importedAt = importedAt;
        this.updatedAt = importedAt;
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

    public UUID getImportBatchId() {
        return importBatchId;
    }

    public String getExternalId() {
        return externalId;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public LocalDate getBookingDate() {
        return bookingDate;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getCurrency() {
        return currency;
    }

    public String getRawHash() {
        return rawHash;
    }

    public BankTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(BankTransactionStatus status) {
        this.status = status;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
