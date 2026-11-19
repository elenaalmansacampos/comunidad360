package com.comunidad360.membership;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MembershipRoleRepository extends JpaRepository<MembershipRole, UUID> {

    @Query("select mr.roleCode from MembershipRole mr where mr.membershipId = :membershipId order by mr.roleCode")
    List<String> findRoleCodesByMembershipId(@Param("membershipId") UUID membershipId);
}
