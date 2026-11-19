package com.comunidad360.chat.dto;

import com.comunidad360.chat.ChatChannel;
import com.comunidad360.chat.ChatChannelType;
import java.time.Instant;
import java.util.UUID;

public record ChatChannelResponse(
        UUID id, ChatChannelType type, String name, String description, Instant createdAt
) {
    public static ChatChannelResponse from(ChatChannel channel) {
        return new ChatChannelResponse(channel.getId(), channel.getType(), channel.getName(),
                channel.getDescription(), channel.getCreatedAt());
    }
}
