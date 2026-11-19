package com.comunidad360.building.dto;

import com.comunidad360.building.Building;

import java.time.Instant;
import java.util.UUID;

public record BuildingResponse(
        UUID id,
        String name,
        String address,
        String city,
        String postalCode,
        Integer floors,
        String notes,
        Instant archivedAt,
        long version
) {
    public static BuildingResponse from(Building building) {
        return new BuildingResponse(
                building.getId(),
                building.getName(),
                building.getAddress(),
                building.getCity(),
                building.getPostalCode(),
                building.getFloors(),
                building.getNotes(),
                building.getArchivedAt(),
                building.getVersion()
        );
    }
}
