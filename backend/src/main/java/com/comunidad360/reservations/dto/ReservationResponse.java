package com.comunidad360.reservations.dto;

import com.comunidad360.reservations.AreaReservation;
import com.comunidad360.reservations.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationResponse(
        UUID id, UUID areaId, String areaName, UUID membershipId, UUID userId, String userName,
        LocalDate date, LocalTime startTime, LocalTime endTime, ReservationStatus status,
        String notes, String reviewedByName, Instant reviewedAt, Instant createdAt, Instant updatedAt,
        boolean mine
) {
    public static ReservationResponse from(AreaReservation reservation, String areaName, String userName,
                                           String reviewedByName, boolean mine) {
        return new ReservationResponse(reservation.getId(), reservation.getAreaId(), areaName,
                reservation.getMembershipId(), reservation.getUserId(), userName,
                reservation.getReservationDate(), reservation.getStartTime(), reservation.getEndTime(),
                reservation.getStatus(), reservation.getNotes(), reviewedByName,
                reservation.getReviewedAt(), reservation.getCreatedAt(), reservation.getUpdatedAt(), mine);
    }
}
