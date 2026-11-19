package com.comunidad360.user.dto;

import com.comunidad360.community.dto.CommunitySummaryResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        String preferredLanguage,
        String status,
        Instant emailVerifiedAt,
        List<CommunitySummaryResponse> memberships
) {
}
