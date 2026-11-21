package com.comunidad360.reports;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.reports.dto.CreateReportScheduleRequest;
import com.comunidad360.reports.dto.ReportScheduleResponse;
import com.comunidad360.reports.dto.UpdateReportScheduleRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportScheduleService {

    private final ReportScheduleRepository repository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;
    private final ReportScheduleRunner runner;

    public ReportScheduleService(ReportScheduleRepository repository,
                                 TenantAccessService tenantAccessService,
                                 AuditService auditService,
                                 ReportScheduleRunner runner) {
        this.repository = repository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
        this.runner = runner;
    }

    @Transactional(readOnly = true)
    public List<ReportScheduleResponse> list(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "REPORT_VIEW");
        return repository.findByCommunityIdOrderByCreatedAtAsc(communityId).stream()
                .map(ReportScheduleResponse::from).toList();
    }

    @Transactional
    public ReportScheduleResponse create(UUID communityId, UUID userId,
                                         CreateReportScheduleRequest request,
                                         HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        Instant now = Instant.now();
        ReportSchedule schedule = repository.saveAndFlush(new ReportSchedule(
                UUID.randomUUID(), communityId, request.name().trim(), request.frequency(),
                recipients(request.recipients()), request.enabled(),
                nextRun(request.frequency(), now), userId, now, now));
        ReportScheduleResponse response = ReportScheduleResponse.from(schedule);
        auditService.record(communityId, userId, "USER", "REPORT_SCHEDULE_CREATED", "REPORT",
                schedule.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public ReportScheduleResponse update(UUID communityId, UUID userId, UUID scheduleId,
                                         UpdateReportScheduleRequest request,
                                         HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        ReportSchedule schedule = find(communityId, scheduleId);
        if (request.version() != null && request.version() != schedule.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        ReportScheduleResponse before = ReportScheduleResponse.from(schedule);
        Instant now = Instant.now();
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            schedule.setName(request.name().trim());
        }
        boolean frequencyChanged = request.frequency() != null && request.frequency() != schedule.getFrequency();
        if (request.frequency() != null) {
            schedule.setFrequency(request.frequency());
        }
        if (request.recipients() != null) {
            schedule.setRecipients(recipients(request.recipients()));
        }
        if (request.enabled() != null) {
            schedule.setEnabled(request.enabled());
        }
        if (frequencyChanged || (schedule.isEnabled() && schedule.getNextRunAt().isBefore(now))) {
            schedule.setNextRunAt(nextRun(schedule.getFrequency(), now));
        }
        schedule.setUpdatedAt(now);
        ReportSchedule saved = repository.saveAndFlush(schedule);
        ReportScheduleResponse response = ReportScheduleResponse.from(saved);
        auditService.record(communityId, userId, "USER", "REPORT_SCHEDULE_UPDATED", "REPORT",
                scheduleId, before, response, httpRequest);
        return response;
    }

    @Transactional
    public void delete(UUID communityId, UUID userId, UUID scheduleId, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        ReportSchedule schedule = find(communityId, scheduleId);
        ReportScheduleResponse before = ReportScheduleResponse.from(schedule);
        repository.delete(schedule);
        auditService.record(communityId, userId, "USER", "REPORT_SCHEDULE_DELETED", "REPORT",
                scheduleId, before, null, httpRequest);
    }

    @Transactional
    public ReportScheduleResponse runNow(UUID communityId, UUID userId, UUID scheduleId) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        find(communityId, scheduleId);
        runner.run(scheduleId, true);
        return ReportScheduleResponse.from(find(communityId, scheduleId));
    }

    public static Instant nextRun(ReportFrequency frequency, Instant from) {
        return switch (frequency) {
            case DAILY -> from.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> from.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> from.plus(30, ChronoUnit.DAYS);
        };
    }

    private ReportSchedule find(UUID communityId, UUID scheduleId) {
        return repository.findByIdAndCommunityId(scheduleId, communityId)
                .orElseThrow(() -> BusinessException.notFound("REPORT_SCHEDULE_NOT_FOUND",
                        "errors.resource.notFound"));
    }

    private String recipients(List<String> values) {
        String joined = values.stream()
                .map(value -> value == null ? "" : value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .distinct()
                .collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            throw BusinessException.unprocessable("RECIPIENTS_REQUIRED", "errors.validation");
        }
        if (joined.length() > 1000) {
            throw BusinessException.unprocessable("RECIPIENTS_TOO_MANY", "errors.validation");
        }
        return joined;
    }
}
