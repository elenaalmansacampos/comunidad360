package com.comunidad360.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20) String taxId
) {
}
