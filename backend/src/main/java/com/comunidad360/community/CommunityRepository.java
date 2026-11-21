package com.comunidad360.community;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommunityRepository extends JpaRepository<Community, UUID> {

    List<Community> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
