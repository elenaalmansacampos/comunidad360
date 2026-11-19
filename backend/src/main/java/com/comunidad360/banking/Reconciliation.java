package com.comunidad360.banking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation")
public class Reconciliation {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "bank_transaction_id", nullable = false)
    private UUID bankTransactionId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    @Column(name = "matched_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal matchedAmount;

    @Column(name = "matched_by_user_id")
    private UUID matchedByUserId;

    @Column(name = "matched_at", nullable = false)
    private Instant matchedAt;

    @Column(length = 500)
    private String notes;

    protected Reconciliation() {
    }

    public Reconciliation(UUID id, UUID communityId, UUID bankTransactionId, UUID paymentId,
                          UUID ledgerEntryId, BigDecimal matchedAmount, UUID matchedByUserId,
                          Instant matchedAt, String notes) {
        this.id = id;
        this.communityId = communityId;
        this.bankTransactionId = bankTransactionId;
        this.paymentId = paymentId;
        this.ledgerEntryId = ledgerEntryId;
        this.matchedAmount = matchedAmount;
        this.matchedByUserId = matchedByUserId;
        this.matchedAt = matchedAt;
        this.notes = notes;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getBankTransactionId() {
        return bankTransactionId;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public UUID getLedgerEntryId() {
        return ledgerEntryId;
    }

    public BigDecimal getMatchedAmount() {
        return matchedAmount;
    }

    public UUID getMatchedByUserId() {
        return matchedByUserId;
    }

    public Instant getMatchedAt() {
        return matchedAt;
    }

    public String getNotes() {
        return notes;
    }
}
