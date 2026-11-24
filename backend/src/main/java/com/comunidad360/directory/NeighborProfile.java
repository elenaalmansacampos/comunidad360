package com.comunidad360.directory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "neighbor_profile")
public class NeighborProfile {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "membership_id", nullable = false, unique = true)
    private UUID membershipId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 40)
    private String phone;

    @Column(name = "show_email", nullable = false)
    private boolean showEmail = true;

    @Column(name = "show_phone", nullable = false)
    private boolean showPhone = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected NeighborProfile() {
    }

    public NeighborProfile(UUID id, UUID communityId, UUID membershipId, UUID userId, String phone,
                           boolean showEmail, boolean showPhone, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.membershipId = membershipId;
        this.userId = userId;
        this.phone = phone;
        this.showEmail = showEmail;
        this.showPhone = showPhone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isShowEmail() {
        return showEmail;
    }

    public void setShowEmail(boolean showEmail) {
        this.showEmail = showEmail;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean showPhone) {
        this.showPhone = showPhone;
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
