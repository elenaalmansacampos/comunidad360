package com.comunidad360.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByCommunityIdAndDocumentIdOrderByVersionNumberDesc(UUID communityId,
                                                                                  UUID documentId);

    Optional<DocumentVersion> findByIdAndCommunityId(UUID id, UUID communityId);

    Optional<DocumentVersion> findFirstByCommunityIdAndDocumentIdOrderByVersionNumberDesc(
            UUID communityId, UUID documentId);
}
