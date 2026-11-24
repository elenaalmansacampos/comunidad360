package com.comunidad360.board.dto;

import com.comunidad360.board.BoardNoteColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBoardNoteRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String body,
        @NotNull BoardNoteColor color
) {
}
