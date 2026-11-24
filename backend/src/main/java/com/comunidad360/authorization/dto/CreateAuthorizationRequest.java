package com.comunidad360.authorization.dto;

import com.comunidad360.authorization.AuthorizationScope;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateAuthorizationRequest(
        @NotNull UUID granteeMembershipId,
        @NotNull AuthorizationScope scope,
        LocalDate validFrom,
        LocalDate validUntil,
        @Size(max = 1000) String notes
) {
}
