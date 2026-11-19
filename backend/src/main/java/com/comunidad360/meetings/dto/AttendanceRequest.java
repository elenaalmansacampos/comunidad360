package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.AttendanceType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttendanceRequest(
        @NotNull UUID membershipId,
        @NotNull AttendanceType attendanceType,
        UUID representedByMembershipId
) {
}
