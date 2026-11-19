package com.comunidad360.meetings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MeetingAttendanceRepository extends JpaRepository<MeetingAttendance, UUID> {

    List<MeetingAttendance> findByCommunityIdAndMeetingIdOrderByRecordedAtAsc(UUID communityId, UUID meetingId);

    Optional<MeetingAttendance> findByMeetingIdAndMembershipId(UUID meetingId, UUID membershipId);
}
