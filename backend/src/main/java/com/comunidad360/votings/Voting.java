package com.comunidad360.votings;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "voting")
public class Voting {
    public static final String LEGAL_NOTICE = "Votación consultiva no vinculante jurídicamente.";
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(nullable = false, length = 300) private String title;
    @Column(length = 2000) private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private VotingStatus status;
    @Column(name = "opens_at") private Instant opensAt;
    @Column(name = "closes_at") private Instant closesAt;
    @Column(name = "legal_notice", nullable = false, length = 2000) private String legalNotice;
    @Column(name = "created_by_user_id") private UUID createdByUserId;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version @Column(nullable = false) private long version;

    protected Voting() {}
    public Voting(UUID id, UUID communityId, String title, String description, Instant opensAt,
                  Instant closesAt, UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id; this.communityId = communityId; this.title = title; this.description = description;
        this.status = VotingStatus.DRAFT; this.opensAt = opensAt; this.closesAt = closesAt;
        this.legalNotice = LEGAL_NOTICE; this.createdByUserId = createdByUserId;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public UUID getCommunityId() { return communityId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public VotingStatus getStatus() { return status; }
    public void setStatus(VotingStatus status) { this.status = status; }
    public Instant getOpensAt() { return opensAt; }
    public void setOpensAt(Instant opensAt) { this.opensAt = opensAt; }
    public Instant getClosesAt() { return closesAt; }
    public void setClosesAt(Instant closesAt) { this.closesAt = closesAt; }
    public String getLegalNotice() { return legalNotice; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public long getVersion() { return version; }
}
