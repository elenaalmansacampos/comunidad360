package com.comunidad360.meetings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingAgendaItemRepository extends JpaRepository<MeetingAgendaItem, UUID> {

    List<MeetingAgendaItem> findByCommunityIdAndMeetingIdOrderByPositionAsc(UUID communityId, UUID meetingId);

    Optional<MeetingAgendaItem> findByIdAndCommunityId(UUID id, UUID communityId);

    long countByCommunityIdAndMeetingId(UUID communityId, UUID meetingId);
}
