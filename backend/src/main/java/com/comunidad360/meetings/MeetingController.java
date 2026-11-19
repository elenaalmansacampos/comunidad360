package com.comunidad360.meetings;

import com.comunidad360.meetings.dto.AgendaItemRequest;
import com.comunidad360.meetings.dto.AgendaItemResponse;
import com.comunidad360.meetings.dto.AgreementRequest;
import com.comunidad360.meetings.dto.AgreementResponse;
import com.comunidad360.meetings.dto.AttendanceRequest;
import com.comunidad360.meetings.dto.AttendanceResponse;
import com.comunidad360.meetings.dto.CreateMeetingRequest;
import com.comunidad360.meetings.dto.MeetingDetailResponse;
import com.comunidad360.meetings.dto.MeetingResponse;
import com.comunidad360.meetings.dto.MinutesRequest;
import com.comunidad360.meetings.dto.UpdateMeetingRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/meetings")
public class MeetingController {

    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @GetMapping
    public List<MeetingResponse> list(@PathVariable UUID communityId,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return meetingService.list(communityId, user.id());
    }

    @PostMapping
    public ResponseEntity<MeetingResponse> create(@PathVariable UUID communityId,
                                                  @AuthenticationPrincipal AuthenticatedUser user,
                                                  @Valid @RequestBody CreateMeetingRequest request,
                                                  HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.create(communityId, user.id(), request, httpRequest));
    }

    @GetMapping("/{meetingId}")
    public MeetingDetailResponse detail(@PathVariable UUID communityId,
                                        @PathVariable UUID meetingId,
                                        @AuthenticationPrincipal AuthenticatedUser user) {
        return meetingService.detail(communityId, user.id(), meetingId);
    }

    @PatchMapping("/{meetingId}")
    public MeetingResponse update(@PathVariable UUID communityId,
                                  @PathVariable UUID meetingId,
                                  @AuthenticationPrincipal AuthenticatedUser user,
                                  @Valid @RequestBody UpdateMeetingRequest request,
                                  HttpServletRequest httpRequest) {
        return meetingService.update(communityId, user.id(), meetingId, request, httpRequest);
    }

    @PostMapping("/{meetingId}/agenda")
    public ResponseEntity<AgendaItemResponse> addAgendaItem(
            @PathVariable UUID communityId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AgendaItemRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.addAgendaItem(communityId, user.id(), meetingId, request, httpRequest));
    }

    @PostMapping("/{meetingId}/attendance")
    public ResponseEntity<AttendanceResponse> recordAttendance(
            @PathVariable UUID communityId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AttendanceRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.recordAttendance(communityId, user.id(), meetingId, request, httpRequest));
    }

    @PostMapping("/{meetingId}/agreements")
    public ResponseEntity<AgreementResponse> addAgreement(
            @PathVariable UUID communityId,
            @PathVariable UUID meetingId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody AgreementRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.addAgreement(communityId, user.id(), meetingId, request, httpRequest));
    }

    @PostMapping("/{meetingId}/minutes")
    public MeetingResponse linkMinutes(@PathVariable UUID communityId,
                                       @PathVariable UUID meetingId,
                                       @AuthenticationPrincipal AuthenticatedUser user,
                                       @Valid @RequestBody MinutesRequest request,
                                       HttpServletRequest httpRequest) {
        return meetingService.linkMinutes(communityId, user.id(), meetingId, request.documentId(), httpRequest);
    }
}
