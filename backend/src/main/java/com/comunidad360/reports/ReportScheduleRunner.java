package com.comunidad360.reports;

import com.comunidad360.audit.AuditService;
import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.ReportService;
import com.comunidad360.notifications.EmailService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReportScheduleRunner {

    private final ReportScheduleRepository repository;
    private final ReportService reportService;
    private final EmailService emailService;
    private final CommunityRepository communityRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    public ReportScheduleRunner(ReportScheduleRepository repository, ReportService reportService,
                                EmailService emailService, CommunityRepository communityRepository,
                                AppUserRepository userRepository, AuditService auditService) {
        this.repository = repository;
        this.reportService = reportService;
        this.emailService = emailService;
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void run(UUID scheduleId, boolean force) {
        ReportSchedule schedule = repository.findById(scheduleId).orElse(null);
        if (schedule == null || (!schedule.isEnabled() && !force)) {
            return;
        }
        Instant now = Instant.now();
        UUID communityId = schedule.getCommunityId();
        Community community = communityRepository.findById(communityId).orElse(null);
        if (community == null || schedule.getCreatedByUserId() == null) {
            finish(schedule, now, ReportRunStatus.SKIPPED);
            return;
        }
        byte[] pdf;
        try {
            pdf = reportService.renderForCommunity(communityId, schedule.getCreatedByUserId());
        } catch (Exception exception) {
            finish(schedule, now, ReportRunStatus.FAILED);
            auditService.record(communityId, schedule.getCreatedByUserId(), "USER",
                    "REPORT_SCHEDULED_FAILED", "REPORT", scheduleId, null,
                    Map.of("reason", exception.getClass().getSimpleName()), null);
            return;
        }
        Locale locale = locale(schedule.getCreatedByUserId());
        String[] recipients = Arrays.stream(schedule.getRecipients().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
        int sent = 0;
        boolean disabled = false;
        for (String recipient : recipients) {
            EmailService.EmailResult result = emailService.sendWithAttachment(recipient,
                    "mail.report.scheduled.subject", "mail.report.scheduled.body",
                    new Object[]{community.getName()}, locale, "informe-comunidad.pdf", pdf, "application/pdf");
            if (result.sent()) {
                sent++;
            } else if ("MAIL_DISABLED".equals(result.errorCode())
                    || "MAIL_NOT_CONFIGURED".equals(result.errorCode())) {
                disabled = true;
            }
        }
        ReportRunStatus status = sent > 0
                ? ReportRunStatus.SENT
                : (disabled ? ReportRunStatus.SKIPPED : ReportRunStatus.FAILED);
        finish(schedule, now, status);
        auditService.record(communityId, schedule.getCreatedByUserId(), "USER",
                "REPORT_SCHEDULED_RUN", "REPORT", scheduleId, null,
                Map.of("status", status.name(), "sent", sent, "recipients", recipients.length), null);
    }

    private void finish(ReportSchedule schedule, Instant now, ReportRunStatus status) {
        schedule.setLastRunAt(now);
        schedule.setLastStatus(status);
        schedule.setNextRunAt(ReportScheduleService.nextRun(schedule.getFrequency(), now));
        schedule.setUpdatedAt(now);
        repository.save(schedule);
    }

    private Locale locale(UUID userId) {
        String language = userRepository.findById(userId)
                .map(AppUser::getPreferredLanguage)
                .filter(value -> value != null && !value.isBlank())
                .orElse("es");
        return Locale.forLanguageTag(language);
    }
}
