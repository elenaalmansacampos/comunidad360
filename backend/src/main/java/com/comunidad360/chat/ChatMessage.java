package com.comunidad360.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message")
public class ChatMessage {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "channel_id", nullable = false) private UUID channelId;
    @Column(name = "sender_user_id") private UUID senderUserId;
    @Column(nullable = false, columnDefinition = "text") private String body;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected ChatMessage() {}
    public ChatMessage(UUID id, UUID communityId, UUID channelId, UUID senderUserId, String body,
                       Instant createdAt) {
        this.id = id; this.communityId = communityId; this.channelId = channelId;
        this.senderUserId = senderUserId; this.body = body; this.createdAt = createdAt;
    }
    public UUID getId() { return id; }
    public UUID getSenderUserId() { return senderUserId; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
