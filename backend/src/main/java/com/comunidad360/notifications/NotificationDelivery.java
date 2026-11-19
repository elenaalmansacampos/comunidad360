package com.comunidad360.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_delivery")
public class NotificationDelivery {

    @Id
    private UUID id;

    @Column(name = "notification_id", nullable = false)
    private UUID notificationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Column(nullable = false)
    private Integer attempt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationDeliveryStatus status;

    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected NotificationDelivery() {
    }

    public NotificationDelivery(UUID id, UUID notificationId, NotificationChannel channel, Integer attempt,
                                NotificationDeliveryStatus status, String providerMessageId,
                                String errorCode, Instant attemptedAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.channel = channel;
        this.attempt = attempt;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.errorCode = errorCode;
        this.attemptedAt = attemptedAt;
    }
}
