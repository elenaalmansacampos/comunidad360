package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.AttendanceType;
import com.comunidad360.meetings.MeetingAttendance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AttendanceResponse(
        UUID id,
        UUID membershipId,
        String memberName,
        AttendanceType attendanceType,
        UUID representedByMembershipId,
        String representedByName,
        BigDecimal coefficientSnapshot,
        Instant recordedAt
) {
    public static AttendanceResponse from(MeetingAttendance attendance, String memberName,
                                          String representedByName) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getMembershipId(),
                memberName,
                attendance.getAttendanceType(),
                attendance.getRepresentedByMembershipId(),
                representedByName,
                attendance.getCoefficientSnapshot(),
                attendance.getRecordedAt()
        );
    }
}
