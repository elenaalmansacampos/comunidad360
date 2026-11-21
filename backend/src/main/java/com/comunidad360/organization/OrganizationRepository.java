package com.comunidad360.organization;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    List<Organization> findAllByOrderByNameAsc();

    Optional<Organization> findByIdAndStatus(UUID id, OrganizationStatus status);
}
