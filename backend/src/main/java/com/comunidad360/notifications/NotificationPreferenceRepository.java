package com.comunidad360.notifications;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    List<NotificationPreference> findByUserIdOrderByCategoryAscChannelAsc(UUID userId);

    Optional<NotificationPreference> findByUserIdAndCommunityIdIsNullAndCategoryAndChannel(
            UUID userId, String category, NotificationChannel channel);
}
