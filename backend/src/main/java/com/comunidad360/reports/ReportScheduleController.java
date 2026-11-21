package com.comunidad360.reports;

import com.comunidad360.reports.dto.CreateReportScheduleRequest;
import com.comunidad360.reports.dto.ReportScheduleResponse;
import com.comunidad360.reports.dto.UpdateReportScheduleRequest;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/report-schedules")
public class ReportScheduleController {

    private final ReportScheduleService service;

    public ReportScheduleController(ReportScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReportScheduleResponse> list(@PathVariable UUID communityId,
                                             @AuthenticationPrincipal AuthenticatedUser user) {
        return service.list(communityId, user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportScheduleResponse create(@PathVariable UUID communityId,
                                         @AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody CreateReportScheduleRequest request,
                                         HttpServletRequest httpRequest) {
        return service.create(communityId, user.id(), request, httpRequest);
    }

    @PatchMapping("/{scheduleId}")
    public ReportScheduleResponse update(@PathVariable UUID communityId, @PathVariable UUID scheduleId,
                                         @AuthenticationPrincipal AuthenticatedUser user,
                                         @Valid @RequestBody UpdateReportScheduleRequest request,
                                         HttpServletRequest httpRequest) {
        return service.update(communityId, user.id(), scheduleId, request, httpRequest);
    }

    @DeleteMapping("/{scheduleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID communityId, @PathVariable UUID scheduleId,
                       @AuthenticationPrincipal AuthenticatedUser user,
                       HttpServletRequest httpRequest) {
        service.delete(communityId, user.id(), scheduleId, httpRequest);
    }

    @PostMapping("/{scheduleId}/run")
    public ReportScheduleResponse run(@PathVariable UUID communityId, @PathVariable UUID scheduleId,
                                      @AuthenticationPrincipal AuthenticatedUser user) {
        return service.runNow(communityId, user.id(), scheduleId);
    }
}
