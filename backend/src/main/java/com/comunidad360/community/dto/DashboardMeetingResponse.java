package com.comunidad360.community.dto;

import com.comunidad360.meetings.MeetingStatus;

import java.time.Instant;
import java.util.UUID;

public record DashboardMeetingResponse(
        UUID id,
        String title,
        MeetingStatus status,
        Instant scheduledStart,
        String location
) {
}
