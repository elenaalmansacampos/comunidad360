package com.comunidad360.incidents.dto;

import com.comunidad360.incidents.Incident;
import com.comunidad360.incidents.IncidentPriority;
import com.comunidad360.incidents.IncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String description,
        String category,
        IncidentPriority priority,
        IncidentStatus status,
        UUID buildingId,
        UUID unitId,
        String unitCode,
        UUID commonElementId,
        UUID reportedByUserId,
        String reportedByName,
        UUID assignedToUserId,
        String assignedToName,
        Instant resolvedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt,
        long version
) {
    public static IncidentResponse from(Incident incident, String unitCode, String reportedByName,
                                        String assignedToName) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getDescription(),
                incident.getCategory(),
                incident.getPriority(),
                incident.getStatus(),
                incident.getBuildingId(),
                incident.getUnitId(),
                unitCode,
                incident.getCommonElementId(),
                incident.getReportedByUserId(),
                reportedByName,
                incident.getAssignedToUserId(),
                assignedToName,
                incident.getResolvedAt(),
                incident.getClosedAt(),
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getVersion()
        );
    }
}
