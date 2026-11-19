package com.comunidad360.meetings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MeetingAgreementRepository extends JpaRepository<MeetingAgreement, UUID> {

    List<MeetingAgreement> findByCommunityIdAndMeetingIdOrderByCreatedAtAsc(UUID communityId, UUID meetingId);
}
