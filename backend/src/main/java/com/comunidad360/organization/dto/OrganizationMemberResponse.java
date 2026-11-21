package com.comunidad360.organization.dto;

import com.comunidad360.organization.OrganizationMemberRole;
import com.comunidad360.organization.OrganizationMemberStatus;

import java.time.Instant;
import java.util.UUID;

public record OrganizationMemberResponse(
        UUID userId, String fullName, String email, OrganizationMemberRole role,
        OrganizationMemberStatus status, Instant createdAt
) {
}
