package com.comunidad360.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatChannelRepository extends JpaRepository<ChatChannel, UUID> {
    List<ChatChannel> findByCommunityIdAndArchivedAtIsNullOrderByTypeAscNameAsc(UUID communityId);
    Optional<ChatChannel> findByIdAndCommunityId(UUID id, UUID communityId);
    Optional<ChatChannel> findFirstByCommunityIdAndTypeAndArchivedAtIsNull(UUID communityId, ChatChannelType type);
}
