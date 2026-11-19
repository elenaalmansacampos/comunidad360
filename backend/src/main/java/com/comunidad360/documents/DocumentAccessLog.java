package com.comunidad360.documents;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_access_log")
public class DocumentAccessLog {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "document_version_id")
    private UUID documentVersionId;

    @Column(name = "user_id")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentAccessAction action;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentAccessLog() {
    }

    public DocumentAccessLog(UUID id, UUID communityId, UUID documentId, UUID documentVersionId,
                             UUID userId, DocumentAccessAction action, String ipHash, String userAgent,
                             Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.documentId = documentId;
        this.documentVersionId = documentVersionId;
        this.userId = userId;
        this.action = action;
        this.ipHash = ipHash;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }
}
