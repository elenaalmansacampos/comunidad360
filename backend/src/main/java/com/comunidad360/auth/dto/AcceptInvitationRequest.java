package com.comunidad360.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInvitationRequest(
        @NotBlank String token,
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Size(min = 12, max = 100) String password
) {
}
