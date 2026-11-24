package com.comunidad360.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BoardNoteRepository extends JpaRepository<BoardNote, UUID> {

    List<BoardNote> findByCommunityIdOrderByPinnedDescCreatedAtDesc(UUID communityId);

    Optional<BoardNote> findByIdAndCommunityId(UUID id, UUID communityId);
}
