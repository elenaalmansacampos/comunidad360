package com.comunidad360.lostfound;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "lost_item_attachment")
public class LostItemAttachment {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "uploaded_by_user_id")
    private UUID uploadedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LostItemAttachment() {
    }

    public LostItemAttachment(UUID id, UUID communityId, UUID itemId, String storageKey, String contentType,
                              String fileName, long sizeBytes, UUID uploadedByUserId, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.itemId = itemId;
        this.storageKey = storageKey;
        this.contentType = contentType;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.uploadedByUserId = uploadedByUserId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCommunityId() {
        return communityId;
    }

    public UUID getItemId() {
        return itemId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public UUID getUploadedByUserId() {
        return uploadedByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
