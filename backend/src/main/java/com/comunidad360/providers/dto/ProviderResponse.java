package com.comunidad360.providers.dto;

import com.comunidad360.providers.Provider;
import com.comunidad360.providers.ProviderStatus;

import java.time.Instant;
import java.util.UUID;

public record ProviderResponse(
        UUID id, String name, String taxId, String category, String email, String phone,
        String address, ProviderStatus status, String notes, Instant createdAt,
        Instant updatedAt, long version
) {
    public static ProviderResponse from(Provider provider) {
        return new ProviderResponse(provider.getId(), provider.getName(), provider.getTaxId(),
                provider.getCategory(), provider.getEmail(), provider.getPhone(), provider.getAddress(),
                provider.getStatus(), provider.getNotes(), provider.getCreatedAt(),
                provider.getUpdatedAt(), provider.getVersion());
    }
}
