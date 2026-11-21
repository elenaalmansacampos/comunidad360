package com.comunidad360.reports.dto;

import com.comunidad360.reports.ReportFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateReportScheduleRequest(
        @Size(max = 200) String name,
        ReportFrequency frequency,
        @NotEmpty List<@Email @Size(max = 320) String> recipients,
        Boolean enabled,
        Long version
) {
}
