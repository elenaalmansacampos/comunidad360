package com.comunidad360.community.dto;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String role,
        Instant expiresAt,
        Instant acceptedAt,
        String status,
        String devToken
) {
}
