package com.comunidad360.notifications.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NotificationPreferencesRequest(
        @NotNull List<@Valid NotificationPreferenceRequest> preferences
) {
}
