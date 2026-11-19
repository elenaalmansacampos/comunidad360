package com.comunidad360.community.dto;

import java.util.List;
import java.util.UUID;

public record MemberResponse(
        UUID membershipId,
        UUID userId,
        String fullName,
        String email,
        String status,
        List<String> roles
) {
}
