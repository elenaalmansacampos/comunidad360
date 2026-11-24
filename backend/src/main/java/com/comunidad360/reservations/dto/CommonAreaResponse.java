package com.comunidad360.reservations.dto;

import com.comunidad360.reservations.CommonArea;

import java.time.Instant;
import java.util.UUID;

public record CommonAreaResponse(
        UUID id, String name, String description, String location, Integer capacity,
        boolean requiresApproval, boolean active, Instant createdAt, long version
) {
    public static CommonAreaResponse from(CommonArea area) {
        return new CommonAreaResponse(area.getId(), area.getName(), area.getDescription(),
                area.getLocation(), area.getCapacity(), area.isRequiresApproval(), area.isActive(),
                area.getCreatedAt(), area.getVersion());
    }
}
