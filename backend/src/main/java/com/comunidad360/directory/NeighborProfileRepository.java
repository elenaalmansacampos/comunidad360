package com.comunidad360.directory;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NeighborProfileRepository extends JpaRepository<NeighborProfile, UUID> {

    List<NeighborProfile> findByCommunityId(UUID communityId);

    Optional<NeighborProfile> findByMembershipId(UUID membershipId);
}
