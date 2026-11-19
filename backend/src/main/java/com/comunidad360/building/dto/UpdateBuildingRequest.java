package com.comunidad360.building.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateBuildingRequest(
        @Size(max = 200) String name,
        @Size(max = 300) String address,
        @Size(max = 100) String city,
        @Size(max = 10) String postalCode,
        @Min(0) Integer floors,
        @Size(max = 2000) String notes,
        Long version
) {
}
