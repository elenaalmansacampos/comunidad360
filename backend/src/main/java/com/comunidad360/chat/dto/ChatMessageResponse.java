package com.comunidad360.chat.dto;

import com.comunidad360.chat.ChatMessage;
import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id, UUID senderUserId, String senderName, String body, Instant createdAt
) {
    public static ChatMessageResponse from(ChatMessage message, String senderName) {
        return new ChatMessageResponse(message.getId(), message.getSenderUserId(), senderName,
                message.getBody(), message.getCreatedAt());
    }
}
