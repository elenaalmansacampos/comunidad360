package com.comunidad360.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentAccessLogRepository extends JpaRepository<DocumentAccessLog, UUID> {

    long countByCommunityIdAndDocumentIdAndAction(UUID communityId, UUID documentId,
                                                  DocumentAccessAction action);
}
