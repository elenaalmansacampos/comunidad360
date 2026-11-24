package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItemStatus;
import com.comunidad360.lostfound.LostItemType;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateLostItemRequest(
        @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LostItemType type,
        @Size(max = 200) String location,
        LocalDate eventDate,
        @Size(max = 200) String contact,
        LostItemStatus status,
        Long version
) {
}
