package com.comunidad360.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    List<Membership> findByUserIdAndStatus(UUID userId, MembershipStatus status);

    List<Membership> findByCommunityIdOrderByCreatedAtAsc(UUID communityId);

    long countByCommunityIdAndStatus(UUID communityId, MembershipStatus status);

    List<Membership> findBySourceOrganizationId(UUID sourceOrganizationId);
}
