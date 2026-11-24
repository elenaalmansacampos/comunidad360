package com.comunidad360.board.dto;

import com.comunidad360.board.BoardNoteComment;

import java.time.Instant;
import java.util.UUID;

public record BoardNoteCommentResponse(
        UUID id, UUID noteId, String body, UUID authorUserId, String authorName, Instant createdAt
) {
    public static BoardNoteCommentResponse from(BoardNoteComment comment, String authorName) {
        return new BoardNoteCommentResponse(comment.getId(), comment.getNoteId(), comment.getBody(),
                comment.getAuthorUserId(), authorName, comment.getCreatedAt());
    }
}
