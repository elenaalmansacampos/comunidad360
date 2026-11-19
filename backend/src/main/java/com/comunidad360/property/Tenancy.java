package com.comunidad360.property;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tenancy")
public class Tenancy {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenancyStatus status;

    @Column(name = "authorized_by_user_id")
    private UUID authorizedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Tenancy() {
    }

    public Tenancy(UUID id, UUID communityId, UUID unitId, UUID userId, LocalDate validFrom, LocalDate validTo,
                   TenancyStatus status, UUID authorizedByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.unitId = unitId;
        this.userId = userId;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.status = status;
        this.authorizedByUserId = authorizedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isActiveOn(LocalDate date) {
        return status == TenancyStatus.ACTIVE
                && !validFrom.isAfter(date)
                && (validTo == null || validTo.isAfter(date));
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public UUID getUserId() {
        return userId;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public TenancyStatus getStatus() {
        return status;
    }

    public void setStatus(TenancyStatus status) {
        this.status = status;
    }

    public UUID getAuthorizedByUserId() {
        return authorizedByUserId;
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
