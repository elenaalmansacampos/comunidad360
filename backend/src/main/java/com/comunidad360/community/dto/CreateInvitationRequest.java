package com.comunidad360.community.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateInvitationRequest(
        @Email @NotBlank String email,
        @NotBlank String role
) {
}
