package com.comunidad360.banking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_account")
public class BankAccount {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "iban_last4", length = 4)
    private String ibanLast4;

    @Column(name = "iban_masked", length = 50)
    private String ibanMasked;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankAccountStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BankAccountSource source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected BankAccount() {
    }

    public BankAccount(UUID id, UUID communityId, String name, String bankName, String ibanLast4,
                       String ibanMasked, BankAccountStatus status, BankAccountSource source,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.name = name;
        this.bankName = bankName;
        this.ibanLast4 = ibanLast4;
        this.ibanMasked = ibanMasked;
        this.status = status;
        this.source = source;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public String getName() {
        return name;
    }

    public String getBankName() {
        return bankName;
    }

    public String getIbanLast4() {
        return ibanLast4;
    }

    public String getIbanMasked() {
        return ibanMasked;
    }

    public String getCurrency() {
        return currency;
    }

    public BankAccountStatus getStatus() {
        return status;
    }

    public BankAccountSource getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
