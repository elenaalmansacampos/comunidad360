package com.comunidad360.chat;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_channel_member")
public class ChatChannelMember {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "channel_id", nullable = false) private UUID channelId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(nullable = false, length = 20) private String role;
    @Column(name = "joined_at", nullable = false) private Instant joinedAt;
    protected ChatChannelMember() {}
    public ChatChannelMember(UUID id, UUID communityId, UUID channelId, UUID membershipId,
                             String role, Instant joinedAt) {
        this.id = id; this.communityId = communityId; this.channelId = channelId;
        this.membershipId = membershipId; this.role = role; this.joinedAt = joinedAt;
    }
    public UUID getChannelId() { return channelId; }
    public UUID getMembershipId() { return membershipId; }
    public String getRole() { return role; }
}
