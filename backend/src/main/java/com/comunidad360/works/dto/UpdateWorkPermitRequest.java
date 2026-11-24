package com.comunidad360.works.dto;

import com.comunidad360.works.WorkPermitStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateWorkPermitRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @Size(max = 200) String location,
        LocalDate startDate,
        LocalDate endDate,
        WorkPermitStatus status,
        @Size(max = 1000) String reviewNotes,
        Long version
) {
}
