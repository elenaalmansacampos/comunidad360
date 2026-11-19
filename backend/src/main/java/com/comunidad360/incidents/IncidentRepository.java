package com.comunidad360.incidents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    List<Incident> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<Incident> findByIdAndCommunityId(UUID id, UUID communityId);
}
