package com.comunidad360.lostfound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_item_note")
public class LostItemNote {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "author_user_id")
    private UUID authorUserId;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LostItemNote() {
    }

    public LostItemNote(UUID id, UUID communityId, UUID itemId, UUID authorUserId, String body, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.itemId = itemId;
        this.authorUserId = authorUserId;
        this.body = body;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public UUID getAuthorUserId() {
        return authorUserId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
