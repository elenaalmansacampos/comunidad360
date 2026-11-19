package com.comunidad360.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatChannelMemberRepository extends JpaRepository<ChatChannelMember, UUID> {
    Optional<ChatChannelMember> findByChannelIdAndMembershipId(UUID channelId, UUID membershipId);
    List<ChatChannelMember> findByChannelId(UUID channelId);
}
