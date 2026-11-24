package com.comunidad360.lostfound;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LostItemAttachmentRepository extends JpaRepository<LostItemAttachment, UUID> {

    List<LostItemAttachment> findByItemIdAndCommunityIdOrderByCreatedAtAsc(UUID itemId, UUID communityId);

    List<LostItemAttachment> findByCommunityIdOrderByCreatedAtAsc(UUID communityId);

    Optional<LostItemAttachment> findByIdAndItemIdAndCommunityId(UUID id, UUID itemId, UUID communityId);
}
