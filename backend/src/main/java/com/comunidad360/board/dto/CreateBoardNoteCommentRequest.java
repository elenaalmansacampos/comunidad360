package com.comunidad360.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBoardNoteCommentRequest(
        @NotBlank @Size(max = 1000) String body
) {
}
