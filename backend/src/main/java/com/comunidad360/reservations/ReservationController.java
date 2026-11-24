package com.comunidad360.reservations;

import com.comunidad360.reservations.dto.CreateReservationRequest;
import com.comunidad360.reservations.dto.ReservationResponse;
import com.comunidad360.reservations.dto.UpdateReservationStatusRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/reservations")
public class ReservationController {

    private final ReservationService service;

    public ReservationController(ReservationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReservationResponse> list(@PathVariable UUID communityId,
                                          @AuthenticationPrincipal AuthenticatedUser user,
                                          @RequestParam(defaultValue = "false") boolean mine,
                                          @RequestParam(required = false) UUID areaId) {
        return service.list(communityId, user.id(), mine, areaId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@PathVariable UUID communityId,
                                      @AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody CreateReservationRequest request,
                                      HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, httpRequest);
    }

    @PatchMapping("/{reservationId}")
    public ReservationResponse review(@PathVariable UUID communityId, @PathVariable UUID reservationId,
                                      @AuthenticationPrincipal AuthenticatedUser user,
                                      @Valid @RequestBody UpdateReservationStatusRequest request,
                                      HttpServletRequest httpRequest) {
        return service.review(communityId, user.id(), reservationId, request, httpRequest);
    }

    @DeleteMapping("/{reservationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID communityId, @PathVariable UUID reservationId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.cancel(communityId, user.id(), reservationId, httpRequest);
    }
}
