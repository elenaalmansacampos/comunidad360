package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderContact;

import java.time.Instant;
import java.util.UUID;

public record ProviderContactResponse(
        UUID id, UUID providerId, String name, String role, String email, String phone,
        boolean primaryContact, Instant createdAt, Instant updatedAt
) {
    public static ProviderContactResponse from(ProviderContact contact) {
        return new ProviderContactResponse(contact.getId(), contact.getProviderId(), contact.getName(),
                contact.getRole(), contact.getEmail(), contact.getPhone(), contact.isPrimaryContact(),
                contact.getCreatedAt(), contact.getUpdatedAt());
    }
}
