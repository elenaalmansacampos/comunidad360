package com.comunidad360.reports.dto;

import com.comunidad360.reports.ReportFrequency;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateReportScheduleRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull ReportFrequency frequency,
        @NotEmpty List<@Email @Size(max = 320) String> recipients,
        boolean enabled
) {
}
