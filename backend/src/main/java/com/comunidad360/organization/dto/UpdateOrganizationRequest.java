package com.comunidad360.organization.dto;

import com.comunidad360.organization.OrganizationStatus;
import jakarta.validation.constraints.Size;

public record UpdateOrganizationRequest(
        @Size(max = 200) String name,
        @Size(max = 20) String taxId,
        OrganizationStatus status
) {
}
