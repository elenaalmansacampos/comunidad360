package com.comunidad360.reservations;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommonAreaRepository extends JpaRepository<CommonArea, UUID> {

    List<CommonArea> findByCommunityIdOrderByNameAsc(UUID communityId);

    List<CommonArea> findByCommunityIdAndActiveTrueOrderByNameAsc(UUID communityId);

    Optional<CommonArea> findByIdAndCommunityId(UUID id, UUID communityId);
}
