package com.comunidad360.providers.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProviderHistoryEntryResponse(
        String type, UUID referenceId, String title, String description,
        String status, BigDecimal amount, Instant date
) {
}
