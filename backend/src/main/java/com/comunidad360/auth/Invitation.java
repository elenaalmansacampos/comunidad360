package com.comunidad360.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invitation")
public class Invitation {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "invited_by_user_id")
    private UUID invitedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Invitation() {
    }

    public Invitation(UUID id, UUID communityId, String email, String roleCode, String tokenHash,
                      Instant expiresAt, UUID invitedByUserId, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.email = email;
        this.roleCode = roleCode;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.invitedByUserId = invitedByUserId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public String getEmail() {
        return email;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(Instant acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public UUID getInvitedByUserId() {
        return invitedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
