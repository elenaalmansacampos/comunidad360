package com.comunidad360.organization.dto;

import java.util.UUID;

public record OrganizationCommunityResponse(
        UUID id, String name, String status
) {
}
