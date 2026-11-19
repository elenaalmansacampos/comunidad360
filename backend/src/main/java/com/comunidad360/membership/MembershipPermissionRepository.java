package com.comunidad360.membership;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MembershipPermissionRepository
        extends JpaRepository<MembershipPermission, MembershipPermissionId> {

    List<MembershipPermission> findByMembershipId(UUID membershipId);
}
