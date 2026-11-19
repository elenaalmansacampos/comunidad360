package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, UUID> {

    List<FiscalYear> findByCommunityIdOrderByYearDesc(UUID communityId);

    Optional<FiscalYear> findByIdAndCommunityId(UUID id, UUID communityId);

    Optional<FiscalYear> findFirstByCommunityIdAndStatusOrderByYearDesc(UUID communityId, FiscalYearStatus status);

    Optional<FiscalYear> findFirstByCommunityIdOrderByYearDesc(UUID communityId);

    boolean existsByCommunityIdAndYear(UUID communityId, Integer year);

    boolean existsByCommunityId(UUID communityId);
}
