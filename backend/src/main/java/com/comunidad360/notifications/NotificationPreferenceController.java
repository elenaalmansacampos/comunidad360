package com.comunidad360.notifications;

import com.comunidad360.notifications.dto.NotificationPreferenceResponse;
import com.comunidad360.notifications.dto.NotificationPreferencesRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationService notificationService;

    public NotificationPreferenceController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<NotificationPreferenceResponse> preferences(
            @AuthenticationPrincipal AuthenticatedUser user) {
        return notificationService.preferences(user.id());
    }

    @PutMapping
    public List<NotificationPreferenceResponse> update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody NotificationPreferencesRequest request) {
        return notificationService.updatePreferences(user.id(), request.preferences());
    }
}
