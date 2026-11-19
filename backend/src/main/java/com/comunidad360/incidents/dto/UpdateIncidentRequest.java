package com.comunidad360.incidents.dto;

import com.comunidad360.incidents.IncidentPriority;
import com.comunidad360.incidents.IncidentStatus;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateIncidentRequest(
        @Size(max = 200) String title,
        @Size(max = 10000) String description,
        @Size(max = 100) String category,
        IncidentPriority priority,
        IncidentStatus status,
        UUID assignedToUserId,
        Long version
) {
}
