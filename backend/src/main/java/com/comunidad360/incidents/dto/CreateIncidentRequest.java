package com.comunidad360.incidents.dto;

import com.comunidad360.incidents.IncidentPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateIncidentRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String description,
        @NotBlank @Size(max = 100) String category,
        @NotNull IncidentPriority priority,
        UUID buildingId,
        UUID unitId,
        UUID commonElementId
) {
}
