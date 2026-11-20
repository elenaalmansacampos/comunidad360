package com.comunidad360.providers.dto;

import com.comunidad360.providers.ProviderNote;

import java.time.Instant;
import java.util.UUID;

public record ProviderNoteResponse(
        UUID id, String body, Instant createdAt
) {
    public static ProviderNoteResponse from(ProviderNote note) {
        return new ProviderNoteResponse(note.getId(), note.getBody(), note.getCreatedAt());
    }
}
