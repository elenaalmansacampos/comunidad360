package com.comunidad360.votings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface VoteRepository extends JpaRepository<Vote, UUID> {
    List<Vote> findByCommunityIdAndVotingId(UUID communityId, UUID votingId);
    long countByCommunityIdAndVotingId(UUID communityId, UUID votingId);
}
