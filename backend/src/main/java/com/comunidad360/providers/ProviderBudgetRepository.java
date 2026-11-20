package com.comunidad360.providers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderBudgetRepository extends JpaRepository<ProviderBudget, UUID> {

    List<ProviderBudget> findByCommunityIdAndProviderIdOrderByCreatedAtDesc(UUID communityId, UUID providerId);

    Optional<ProviderBudget> findByIdAndCommunityIdAndProviderId(UUID id, UUID communityId, UUID providerId);
}
