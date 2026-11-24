package com.comunidad360.authorization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {

    Optional<Authorization> findByIdAndCommunityId(UUID id, UUID communityId);

    List<Authorization> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    List<Authorization> findByCommunityIdAndGrantorMembershipIdOrderByCreatedAtDesc(UUID communityId,
                                                                                   UUID grantorMembershipId);

    List<Authorization> findByCommunityIdAndGranteeMembershipIdOrderByCreatedAtDesc(UUID communityId,
                                                                                   UUID granteeMembershipId);

    Optional<Authorization> findFirstByCommunityIdAndGrantorMembershipIdAndGranteeMembershipIdAndScopeAndStatusOrderByCreatedAtDesc(
            UUID communityId, UUID grantorMembershipId, UUID granteeMembershipId,
            AuthorizationScope scope, AuthorizationStatus status);

    boolean existsByCommunityIdAndGrantorMembershipIdAndGranteeMembershipIdAndScopeAndStatus(
            UUID communityId, UUID grantorMembershipId, UUID granteeMembershipId,
            AuthorizationScope scope, AuthorizationStatus status);
}
