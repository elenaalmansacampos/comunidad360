package com.comunidad360.finance;

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
@Table(name = "ledger_entry")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "fiscal_year_id", nullable = false)
    private UUID fiscalYearId;

    @Column(name = "entry_number", nullable = false)
    private Long entryNumber;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryType type;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryStatus status;

    @Column(name = "reversal_of_id")
    private UUID reversalOfId;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected LedgerEntry() {
    }

    public LedgerEntry(UUID id, UUID communityId, UUID fiscalYearId, Long entryNumber, LocalDate entryDate,
                       LedgerEntryType type, String category, String description, BigDecimal amount,
                       LedgerEntryStatus status, UUID reversalOfId, UUID createdByUserId,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.fiscalYearId = fiscalYearId;
        this.entryNumber = entryNumber;
        this.entryDate = entryDate;
        this.type = type;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.status = status;
        this.reversalOfId = reversalOfId;
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getFiscalYearId() {
        return fiscalYearId;
    }

    public Long getEntryNumber() {
        return entryNumber;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public LedgerEntryType getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LedgerEntryStatus getStatus() {
        return status;
    }

    public void setStatus(LedgerEntryStatus status) {
        this.status = status;
    }

    public UUID getReversalOfId() {
        return reversalOfId;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
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
