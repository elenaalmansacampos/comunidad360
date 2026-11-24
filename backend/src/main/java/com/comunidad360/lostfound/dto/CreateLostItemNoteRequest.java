package com.comunidad360.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLostItemNoteRequest(@NotBlank @Size(max = 1000) String body) {
}
