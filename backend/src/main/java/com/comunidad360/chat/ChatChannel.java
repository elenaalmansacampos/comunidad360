package com.comunidad360.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_channel")
public class ChatChannel {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ChatChannelType type;
    @Column(length = 200) private String name;
    @Column(length = 2000) private String description;
    @Column(name = "created_by_user_id") private UUID createdByUserId;
    @Column(name = "archived_at") private Instant archivedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected ChatChannel() {}
    public ChatChannel(UUID id, UUID communityId, ChatChannelType type, String name, String description,
                       UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id; this.communityId = communityId; this.type = type; this.name = name;
        this.description = description; this.createdByUserId = createdByUserId;
        this.createdAt = createdAt; this.updatedAt = updatedAt;
    }
    public UUID getId() { return id; }
    public ChatChannelType getType() { return type; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Instant getArchivedAt() { return archivedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
