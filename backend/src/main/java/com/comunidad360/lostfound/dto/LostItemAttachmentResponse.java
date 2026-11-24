package com.comunidad360.lostfound.dto;

import com.comunidad360.lostfound.LostItemAttachment;

import java.time.Instant;
import java.util.UUID;

public record LostItemAttachmentResponse(
        UUID id, UUID itemId, String fileName, String contentType, long sizeBytes,
        UUID uploadedByUserId, String uploadedByName, String downloadUrl, Instant createdAt
) {
    public static LostItemAttachmentResponse from(LostItemAttachment attachment, String uploadedByName) {
        String downloadUrl = "/communities/" + attachment.getCommunityId() + "/lost-items/"
                + attachment.getItemId() + "/attachments/" + attachment.getId();
        return new LostItemAttachmentResponse(attachment.getId(), attachment.getItemId(),
                attachment.getFileName(), attachment.getContentType(), attachment.getSizeBytes(),
                attachment.getUploadedByUserId(), uploadedByName, downloadUrl, attachment.getCreatedAt());
    }
}
