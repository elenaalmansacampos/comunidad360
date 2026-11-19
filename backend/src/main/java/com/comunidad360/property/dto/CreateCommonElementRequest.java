package com.comunidad360.property.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCommonElementRequest(
        UUID buildingId,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50) String type,
        @Size(max = 2000) String description
) {
}
