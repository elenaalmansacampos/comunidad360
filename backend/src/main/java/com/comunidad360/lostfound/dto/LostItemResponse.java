package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItem;
import com.comunidad360.lostfound.LostItemStatus;
import com.comunidad360.lostfound.LostItemType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record LostItemResponse(
        UUID id, String title, String description, LostItemType type, String location,
        LocalDate eventDate, String contact, LostItemStatus status, UUID createdByUserId,
        String createdByName, String photoUrl, Instant createdAt, Instant updatedAt, long version,
        List<LostItemNoteResponse> notes, List<LostItemAttachmentResponse> attachments
) {
    public static LostItemResponse from(LostItem item, String createdByName) {
        return from(item, createdByName, List.of(), List.of());
    }

    public static LostItemResponse from(LostItem item, String createdByName,
                                        List<LostItemNoteResponse> notes,
                                        List<LostItemAttachmentResponse> attachments) {
        String photoUrl = item.getPhotoKey() == null ? null
                : "/communities/" + item.getCommunityId() + "/lost-items/" + item.getId() + "/photo";
        return new LostItemResponse(item.getId(), item.getTitle(), item.getDescription(), item.getType(),
                item.getLocation(), item.getEventDate(), item.getContact(), item.getStatus(),
                item.getCreatedByUserId(), createdByName, photoUrl, item.getCreatedAt(), item.getUpdatedAt(),
                item.getVersion(), notes, attachments);
    }
}
