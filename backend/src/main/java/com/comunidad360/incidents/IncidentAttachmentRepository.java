package com.comunidad360.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentAttachmentRepository extends JpaRepository<IncidentAttachment, UUID> {

    List<IncidentAttachment> findByCommunityIdAndIncidentIdOrderByCreatedAtAsc(UUID communityId, UUID incidentId);

    Optional<IncidentAttachment> findByIdAndCommunityId(UUID id, UUID communityId);
}
