package com.comunidad360.incidents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_comment")
public class IncidentComment {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(nullable = false, length = 5000)
    private String body;

    @Column(name = "is_internal", nullable = false)
    private boolean internal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IncidentComment() {
    }

    public IncidentComment(UUID id, UUID communityId, UUID incidentId, UUID authorUserId, String body,
                           boolean internal, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.communityId = communityId;
        this.incidentId = incidentId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.internal = internal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getIncidentId() {
        return incidentId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }

    public boolean isInternal() {
        return internal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
