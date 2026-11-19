package com.comunidad360.notifications;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification")
public class Notification {

    @Id
    private UUID id;

    @Column(name = "community_id")
    private UUID communityId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String type;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(name = "template_key", nullable = false, length = 120)
    private String templateKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> params = new HashMap<>();

    @Column(nullable = false, length = 10)
    private String locale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    protected Notification() {
    }

    public Notification(UUID id, UUID communityId, UUID userId, String type, String category,
                        String templateKey, Map<String, Object> params, String locale,
                        NotificationChannel channel, NotificationStatus status, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.userId = userId;
        this.type = type;
        this.category = category;
        this.templateKey = templateKey;
        this.params = new HashMap<>(params);
        this.locale = locale;
        this.channel = channel;
        this.status = status;
        this.createdAt = createdAt;
        if (status == NotificationStatus.SENT) {
            this.sentAt = createdAt;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public String getTemplateKey() {
        return templateKey;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public String getLocale() {
        return locale;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void markRead(Instant readAt) {
        this.status = NotificationStatus.READ;
        this.readAt = readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
