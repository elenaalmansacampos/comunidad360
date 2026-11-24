package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateLostItemRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        @NotNull LostItemType type,
        @Size(max = 200) String location,
        LocalDate eventDate,
        @Size(max = 200) String contact
) {
}
