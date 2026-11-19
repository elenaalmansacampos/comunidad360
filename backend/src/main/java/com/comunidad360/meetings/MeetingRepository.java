package com.comunidad360.meetings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingRepository extends JpaRepository<Meeting, UUID> {

    List<Meeting> findByCommunityIdOrderByScheduledStartDesc(UUID communityId);

    Optional<Meeting> findByIdAndCommunityId(UUID id, UUID communityId);

    List<Meeting> findTop5ByCommunityIdAndStatusAndScheduledStartAfterOrderByScheduledStartAsc(
            UUID communityId, MeetingStatus status, java.time.Instant after);
}
