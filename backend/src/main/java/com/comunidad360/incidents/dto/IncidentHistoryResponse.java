package com.comunidad360.incidents.dto;

import com.comunidad360.incidents.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentHistoryResponse(
        UUID id,
        IncidentStatus fromStatus,
        IncidentStatus toStatus,
        String changedByName,
        Instant changedAt,
        String note
) {
}
