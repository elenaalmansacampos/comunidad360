package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItemNote;

import java.time.Instant;
import java.util.UUID;

public record LostItemNoteResponse(
        UUID id, UUID itemId, String body, UUID authorUserId, String authorName, Instant createdAt
) {
    public static LostItemNoteResponse from(LostItemNote note, String authorName) {
        return new LostItemNoteResponse(note.getId(), note.getItemId(), note.getBody(),
                note.getAuthorUserId(), authorName, note.getCreatedAt());
    }
}
