package com.comunidad360.votings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VotingOptionRepository extends JpaRepository<VotingOption, UUID> {
    List<VotingOption> findByCommunityIdAndVotingIdOrderByPositionAsc(UUID communityId, UUID votingId);
    Optional<VotingOption> findByIdAndCommunityId(UUID id, UUID communityId);
}
