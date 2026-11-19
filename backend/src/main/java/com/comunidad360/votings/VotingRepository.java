package com.comunidad360.votings;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VotingRepository extends JpaRepository<Voting, UUID> {
    List<Voting> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);
    Optional<Voting> findByIdAndCommunityId(UUID id, UUID communityId);
}
