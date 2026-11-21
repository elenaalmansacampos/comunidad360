package com.comunidad360.organization.dto;

import com.comunidad360.organization.OrganizationMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddOrganizationMemberRequest(
        @NotBlank @Email String email,
        @NotNull OrganizationMemberRole role
) {
}
