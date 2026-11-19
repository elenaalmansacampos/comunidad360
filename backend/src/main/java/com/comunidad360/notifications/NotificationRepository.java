package com.comunidad360.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUserIdAndStatusNotOrderByCreatedAtDesc(UUID userId, NotificationStatus status);

    List<Notification> findByUserIdAndChannelOrderByCreatedAtDesc(UUID userId, NotificationChannel channel);

    List<Notification> findByUserIdAndChannelAndStatusNotOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, NotificationStatus status);

    long countByUserIdAndChannelAndStatusNot(UUID userId, NotificationChannel channel,
                                             NotificationStatus status);

    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);

    @Modifying
    @Query("""
            update Notification n
               set n.status = com.comunidad360.notifications.NotificationStatus.READ,
                   n.readAt = :readAt
             where n.userId = :userId
               and n.status <> com.comunidad360.notifications.NotificationStatus.READ
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);
}
