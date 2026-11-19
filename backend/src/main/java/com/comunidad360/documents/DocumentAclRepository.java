package com.comunidad360.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentAclRepository extends JpaRepository<DocumentAcl, UUID> {

    List<DocumentAcl> findByCommunityIdAndDocumentId(UUID communityId, UUID documentId);
}
