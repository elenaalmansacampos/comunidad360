package com.comunidad360.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository extends JpaRepository<Unit, UUID> {

    List<Unit> findByCommunityIdAndArchivedAtIsNullOrderByCodeAsc(UUID communityId);

    Optional<Unit> findByIdAndCommunityId(UUID id, UUID communityId);

    boolean existsByCommunityIdAndCode(UUID communityId, String code);

    boolean existsByCommunityIdAndCodeAndIdNot(UUID communityId, String code, UUID id);

    long countByCommunityIdAndArchivedAtIsNull(UUID communityId);

    boolean existsByCommunityIdAndBuildingIdAndArchivedAtIsNull(UUID communityId, UUID buildingId);
}
