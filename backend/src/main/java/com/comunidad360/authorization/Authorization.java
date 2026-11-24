package com.comunidad360.authorization;

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
@Table(name = "community_authorization")
public class Authorization {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "grantor_membership_id", nullable = false)
    private UUID grantorMembershipId;

    @Column(name = "grantee_membership_id", nullable = false)
    private UUID granteeMembershipId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AuthorizationScope scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthorizationStatus status;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_by_user_id")
    private UUID createdByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Authorization() {
    }

    public Authorization(UUID id, UUID communityId, UUID grantorMembershipId, UUID granteeMembershipId,
                         AuthorizationScope scope, AuthorizationStatus status, LocalDate validFrom,
                         LocalDate validUntil, String notes, UUID createdByUserId, Instant createdAt,
                         Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.grantorMembershipId = grantorMembershipId;
        this.granteeMembershipId = granteeMembershipId;
        this.scope = scope;
        this.status = status;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.notes = notes;
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

    public UUID getGrantorMembershipId() {
        return grantorMembershipId;
    }

    public UUID getGranteeMembershipId() {
        return granteeMembershipId;
    }

    public AuthorizationScope getScope() {
        return scope;
    }

    public AuthorizationStatus getStatus() {
        return status;
    }

    public void setStatus(AuthorizationStatus status) {
        this.status = status;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
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

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isEffective(LocalDate today) {
        return status == AuthorizationStatus.ACTIVE
                && !validFrom.isAfter(today)
                && (validUntil == null || !validUntil.isBefore(today));
    }
}
