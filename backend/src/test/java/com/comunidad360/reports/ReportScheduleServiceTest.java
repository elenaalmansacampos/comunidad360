package com.comunidad360.reports;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ReportScheduleServiceTest {

    @Test
    void nextRunUsesFrequencyInterval() {
        Instant from = Instant.parse("2026-11-21T08:00:00Z");

        assertThat(ReportScheduleService.nextRun(ReportFrequency.DAILY, from))
                .isEqualTo(from.plusSeconds(24 * 60 * 60));
        assertThat(ReportScheduleService.nextRun(ReportFrequency.WEEKLY, from))
                .isEqualTo(from.plusSeconds(7 * 24 * 60 * 60));
        assertThat(ReportScheduleService.nextRun(ReportFrequency.MONTHLY, from))
                .isEqualTo(from.plusSeconds(30 * 24 * 60 * 60));
    }
}
