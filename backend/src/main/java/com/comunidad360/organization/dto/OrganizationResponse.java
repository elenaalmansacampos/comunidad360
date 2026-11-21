package com.comunidad360.organization.dto;

import com.comunidad360.organization.OrganizationMemberRole;
import com.comunidad360.organization.OrganizationStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationResponse(
        UUID id, String name, String taxId, OrganizationStatus status,
        OrganizationMemberRole role, long communityCount, Instant createdAt
) {
}
