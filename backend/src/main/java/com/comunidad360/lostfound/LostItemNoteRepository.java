package com.comunidad360.lostfound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostItemNoteRepository extends JpaRepository<LostItemNote, UUID> {

    List<LostItemNote> findByItemIdAndCommunityIdOrderByCreatedAtAsc(UUID itemId, UUID communityId);

    List<LostItemNote> findByCommunityIdOrderByCreatedAtAsc(UUID communityId);

    Optional<LostItemNote> findByIdAndItemIdAndCommunityId(UUID id, UUID itemId, UUID communityId);
}
