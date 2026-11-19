package com.comunidad360.votings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VotingEligibilityRepository extends JpaRepository<VotingEligibility, UUID> {
    Optional<VotingEligibility> findByVotingIdAndMembershipId(UUID votingId, UUID membershipId);
    List<VotingEligibility> findByCommunityIdAndVotingId(UUID communityId, UUID votingId);
}
