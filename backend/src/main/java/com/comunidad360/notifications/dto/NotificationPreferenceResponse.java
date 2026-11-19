package com.comunidad360.notifications.dto;

import com.comunidad360.notifications.NotificationChannel;

public record NotificationPreferenceResponse(
        String category,
        NotificationChannel channel,
        boolean enabled
) {
}
