package com.comunidad360.property.dto;

import com.comunidad360.property.CommonElement;

import java.time.Instant;
import java.util.UUID;

public record CommonElementResponse(
        UUID id,
        UUID buildingId,
        String buildingName,
        String name,
        String type,
        String description,
        Instant archivedAt
) {
    public static CommonElementResponse from(CommonElement element, String buildingName) {
        return new CommonElementResponse(
                element.getId(),
                element.getBuildingId(),
                buildingName,
                element.getName(),
                element.getType(),
                element.getDescription(),
                element.getArchivedAt()
        );
    }
}
