package com.comunidad360.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BoardNoteCommentRepository extends JpaRepository<BoardNoteComment, UUID> {

    List<BoardNoteComment> findByCommunityIdAndNoteIdOrderByCreatedAtAsc(UUID communityId, UUID noteId);

    long countByCommunityIdAndNoteId(UUID communityId, UUID noteId);
}
