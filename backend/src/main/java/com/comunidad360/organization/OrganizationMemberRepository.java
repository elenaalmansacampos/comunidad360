package com.comunidad360.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    List<OrganizationMember> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);

    List<OrganizationMember> findByUserIdAndStatusOrderByCreatedAtAsc(UUID userId, OrganizationMemberStatus status);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    long countByOrganizationIdAndRoleAndStatus(UUID organizationId, OrganizationMemberRole role,
                                               OrganizationMemberStatus status);
}
