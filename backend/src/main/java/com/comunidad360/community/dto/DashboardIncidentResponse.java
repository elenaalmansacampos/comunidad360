package com.comunidad360.community.dto;

import com.comunidad360.incidents.IncidentPriority;
import com.comunidad360.incidents.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record DashboardIncidentResponse(
        UUID id,
        String title,
        IncidentStatus status,
        IncidentPriority priority,
        String unitCode,
        Instant createdAt
) {
}
