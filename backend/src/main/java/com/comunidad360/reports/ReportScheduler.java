package com.comunidad360.reports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "app.reports.scheduled-enabled", havingValue = "true", matchIfMissing = true)
public class ReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReportScheduler.class);

    private final ReportScheduleRepository repository;
    private final ReportScheduleRunner runner;

    public ReportScheduler(ReportScheduleRepository repository, ReportScheduleRunner runner) {
        this.repository = repository;
        this.runner = runner;
    }

    @Scheduled(fixedDelayString = "${app.reports.scheduler-delay-ms:60000}")
    public void runDue() {
        for (UUID scheduleId : repository.findDueIds(Instant.now())) {
            try {
                runner.run(scheduleId, false);
            } catch (Exception exception) {
                log.error("No se pudo ejecutar el informe programado {}: {}", scheduleId, exception.toString());
            }
        }
    }
}
