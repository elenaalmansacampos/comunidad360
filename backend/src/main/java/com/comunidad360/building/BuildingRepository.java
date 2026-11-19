package com.comunidad360.building;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findByCommunityIdAndArchivedAtIsNullOrderByNameAsc(UUID communityId);

    Optional<Building> findByIdAndCommunityId(UUID id, UUID communityId);

    long countByCommunityIdAndArchivedAtIsNull(UUID communityId);

    boolean existsByIdAndCommunityId(UUID id, UUID communityId);
}
