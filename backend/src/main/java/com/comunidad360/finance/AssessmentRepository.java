package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {

    List<Assessment> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    List<Assessment> findByCommunityIdAndFiscalYearIdOrderByCreatedAtDesc(UUID communityId, UUID fiscalYearId);

    Optional<Assessment> findByIdAndCommunityId(UUID id, UUID communityId);
}
