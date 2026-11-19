package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByCommunityIdOrderByCreatedAtDesc(UUID communityId);

    Optional<Budget> findByIdAndCommunityId(UUID id, UUID communityId);

    List<Budget> findByCommunityIdAndFiscalYearIdOrderByCreatedAtDesc(UUID communityId, UUID fiscalYearId);
}
