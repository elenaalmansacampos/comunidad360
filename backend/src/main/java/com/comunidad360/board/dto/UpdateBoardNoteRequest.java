package com.comunidad360.board.dto;

import com.comunidad360.board.BoardNoteColor;
import jakarta.validation.constraints.Size;

public record UpdateBoardNoteRequest(
        @Size(max = 120) String title,
        @Size(max = 2000) String body,
        BoardNoteColor color,
        Boolean pinned,
        Long version
) {
}
