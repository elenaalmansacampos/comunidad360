package com.comunidad360.lostfound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostItemRepository extends JpaRepository<LostItem, UUID> {

    List<LostItem> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<LostItem> findByIdAndCommunityId(UUID id, UUID communityId);
}
