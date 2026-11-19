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
@Table(name = "document_acl")
public class DocumentAcl {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 20)
    private DocumentAclPrincipalType principalType;

    @Column(name = "principal_id", nullable = false, length = 100)
    private String principalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentAclPermission permission;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentAcl() {
    }

    public DocumentAcl(UUID id, UUID communityId, UUID documentId, DocumentAclPrincipalType principalType,
                       String principalId, DocumentAclPermission permission, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.documentId = documentId;
        this.principalType = principalType;
        this.principalId = principalId;
        this.permission = permission;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public DocumentAclPrincipalType getPrincipalType() {
        return principalType;
    }

    public String getPrincipalId() {
        return principalId;
    }

    public DocumentAclPermission getPermission() {
        return permission;
    }
}
