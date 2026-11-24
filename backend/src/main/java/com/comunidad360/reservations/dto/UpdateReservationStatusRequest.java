package com.comunidad360.reservations.dto;

import com.comunidad360.reservations.ReservationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateReservationStatusRequest(@NotNull ReservationStatus status) {
}
