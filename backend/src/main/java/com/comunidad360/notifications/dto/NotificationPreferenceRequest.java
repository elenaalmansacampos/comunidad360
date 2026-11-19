package com.comunidad360.notifications.dto;

import com.comunidad360.notifications.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
        @NotBlank String category,
        @NotNull NotificationChannel channel,
        boolean enabled
) {
}
