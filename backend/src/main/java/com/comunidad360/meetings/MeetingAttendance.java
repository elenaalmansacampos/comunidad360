package com.comunidad360.meetings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_attendance")
public class MeetingAttendance {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type", nullable = false, length = 20)
    private AttendanceType attendanceType;

    @Column(name = "represented_by_membership_id")
    private UUID representedByMembershipId;

    @Column(name = "coefficient_snapshot", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientSnapshot;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected MeetingAttendance() {
    }

    public MeetingAttendance(UUID id, UUID communityId, UUID meetingId, UUID membershipId,
                             AttendanceType attendanceType, UUID representedByMembershipId,
                             BigDecimal coefficientSnapshot, Instant recordedAt) {
        this.id = id;
        this.communityId = communityId;
        this.meetingId = meetingId;
        this.membershipId = membershipId;
        this.attendanceType = attendanceType;
        this.representedByMembershipId = representedByMembershipId;
        this.coefficientSnapshot = coefficientSnapshot;
        this.recordedAt = recordedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public AttendanceType getAttendanceType() {
        return attendanceType;
    }

    public void setAttendanceType(AttendanceType attendanceType) {
        this.attendanceType = attendanceType;
    }

    public UUID getRepresentedByMembershipId() {
        return representedByMembershipId;
    }

    public void setRepresentedByMembershipId(UUID representedByMembershipId) {
        this.representedByMembershipId = representedByMembershipId;
    }

    public BigDecimal getCoefficientSnapshot() {
        return coefficientSnapshot;
    }

    public void setCoefficientSnapshot(BigDecimal coefficientSnapshot) {
        this.coefficientSnapshot = coefficientSnapshot;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }
}
