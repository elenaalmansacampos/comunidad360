package com.comunidad360.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenancyRepository extends JpaRepository<Tenancy, UUID> {

    List<Tenancy> findByCommunityIdAndUnitIdOrderByValidFromDesc(UUID communityId, UUID unitId);

    Optional<Tenancy> findByIdAndCommunityIdAndUnitId(UUID id, UUID communityId, UUID unitId);

    List<Tenancy> findByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId, TenancyStatus status);

    boolean existsByCommunityIdAndUnitIdAndUserIdAndStatus(UUID communityId, UUID unitId, UUID userId,
                                                           TenancyStatus status);
}
