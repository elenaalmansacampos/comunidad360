package com.comunidad360.notifications.dto;

import com.comunidad360.notifications.Notification;
import com.comunidad360.notifications.NotificationChannel;
import com.comunidad360.notifications.NotificationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID communityId,
        String type,
        String category,
        String templateKey,
        Map<String, Object> params,
        String locale,
        NotificationChannel channel,
        NotificationStatus status,
        Instant readAt,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getCommunityId(),
                notification.getType(),
                notification.getCategory(),
                notification.getTemplateKey(),
                notification.getParams(),
                notification.getLocale(),
                notification.getChannel(),
                notification.getStatus(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
