package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.MeetingStatus;
import com.comunidad360.meetings.MeetingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record CreateMeetingRequest(
        @NotBlank @Size(max = 300) String title,
        @NotNull MeetingType type,
        @NotNull Instant scheduledStart,
        Instant scheduledEnd,
        @Size(max = 300) String location,
        MeetingStatus status,
        UUID convocationDocumentId
) {
}
