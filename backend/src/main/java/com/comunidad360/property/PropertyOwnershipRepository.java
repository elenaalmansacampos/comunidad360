package com.comunidad360.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyOwnershipRepository extends JpaRepository<PropertyOwnership, UUID> {

    List<PropertyOwnership> findByCommunityIdAndUnitIdOrderByValidFromDesc(UUID communityId, UUID unitId);

    Optional<PropertyOwnership> findByIdAndCommunityIdAndUnitId(UUID id, UUID communityId, UUID unitId);

    List<PropertyOwnership> findByCommunityIdAndUserIdOrderByValidFromDesc(UUID communityId, UUID userId);

    boolean existsByCommunityIdAndUnitIdAndUserIdAndValidToIsNull(UUID communityId, UUID unitId, UUID userId);
}
