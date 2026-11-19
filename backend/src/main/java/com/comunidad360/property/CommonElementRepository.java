package com.comunidad360.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommonElementRepository extends JpaRepository<CommonElement, UUID> {

    List<CommonElement> findByCommunityIdAndArchivedAtIsNullOrderByNameAsc(UUID communityId);

    Optional<CommonElement> findByIdAndCommunityId(UUID id, UUID communityId);

    long countByCommunityIdAndArchivedAtIsNull(UUID communityId);
}
