package com.comunidad360.board.dto;

import com.comunidad360.board.BoardNote;
import com.comunidad360.board.BoardNoteColor;

import java.time.Instant;
import java.util.UUID;

public record BoardNoteResponse(
        UUID id, String title, String body, BoardNoteColor color, boolean pinned,
        UUID createdByUserId, String createdByName, Instant createdAt, Instant updatedAt, long version
) {
    public static BoardNoteResponse from(BoardNote note, String createdByName) {
        return new BoardNoteResponse(note.getId(), note.getTitle(), note.getBody(), note.getColor(),
                note.isPinned(), note.getCreatedByUserId(), createdByName, note.getCreatedAt(),
                note.getUpdatedAt(), note.getVersion());
    }
}
