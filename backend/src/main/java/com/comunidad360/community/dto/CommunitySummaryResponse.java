package com.comunidad360.community.dto;

import java.util.List;
import java.util.UUID;

public record CommunitySummaryResponse(
        UUID id,
        String name,
        List<String> roles,
        String status
) {
}
