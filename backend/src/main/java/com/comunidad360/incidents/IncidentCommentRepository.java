package com.comunidad360.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IncidentCommentRepository extends JpaRepository<IncidentComment, UUID> {

    List<IncidentComment> findByCommunityIdAndIncidentIdOrderByCreatedAtAsc(UUID communityId, UUID incidentId);
}
