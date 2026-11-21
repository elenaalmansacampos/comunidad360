package com.comunidad360.reports.dto;

import com.comunidad360.reports.ReportFrequency;
import com.comunidad360.reports.ReportRunStatus;
import com.comunidad360.reports.ReportSchedule;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record ReportScheduleResponse(
        UUID id, String name, ReportFrequency frequency, List<String> recipients,
        boolean enabled, Instant lastRunAt, Instant nextRunAt, ReportRunStatus lastStatus, long version
) {
    public static ReportScheduleResponse from(ReportSchedule schedule) {
        List<String> recipients = Arrays.stream(schedule.getRecipients().split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
        return new ReportScheduleResponse(schedule.getId(), schedule.getName(), schedule.getFrequency(),
                recipients, schedule.isEnabled(), schedule.getLastRunAt(), schedule.getNextRunAt(),
                schedule.getLastStatus(), schedule.getVersion());
    }
}
