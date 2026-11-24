package com.comunidad360.works;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkPermitRepository extends JpaRepository<WorkPermit, UUID> {

    List<WorkPermit> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<WorkPermit> findByIdAndCommunityId(UUID id, UUID communityId);
}
