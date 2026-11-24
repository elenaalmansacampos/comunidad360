package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItem;
import com.comunidad360.lostfound.LostItemStatus;
import com.comunidad360.lostfound.LostItemType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LostItemResponse(
        UUID id, String title, String description, LostItemType type, String location,
        LocalDate eventDate, String contact, LostItemStatus status, UUID createdByUserId,
        String createdByName, Instant createdAt, Instant updatedAt, long version
) {
    public static LostItemResponse from(LostItem item, String createdByName) {
        return new LostItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getType(),
                item.getLocation(), item.getEventDate(), item.getContact(), item.getStatus(),
                item.getCreatedByUserId(), createdByName, item.getCreatedAt(), item.getUpdatedAt(),
                item.getVersion());
    }
}
