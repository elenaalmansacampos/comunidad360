package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.Meeting;
import com.comunidad360.meetings.MeetingStatus;
import com.comunidad360.meetings.MeetingType;

import java.time.Instant;
import java.util.UUID;

public record MeetingResponse(
        UUID id,
        String title,
        MeetingType type,
        Instant scheduledStart,
        Instant scheduledEnd,
        String location,
        MeetingStatus status,
        UUID convocationDocumentId,
        UUID minutesDocumentId,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.getId(),
                meeting.getTitle(),
                meeting.getType(),
                meeting.getScheduledStart(),
                meeting.getScheduledEnd(),
                meeting.getLocation(),
                meeting.getStatus(),
                meeting.getConvocationDocumentId(),
                meeting.getMinutesDocumentId(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt(),
                meeting.getVersion()
        );
    }
}
