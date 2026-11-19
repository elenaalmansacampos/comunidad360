package com.comunidad360.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<Document> findByIdAndCommunityId(UUID id, UUID communityId);
}
