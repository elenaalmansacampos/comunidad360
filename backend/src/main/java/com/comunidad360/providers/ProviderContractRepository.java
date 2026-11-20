package com.comunidad360.providers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderContractRepository extends JpaRepository<ProviderContract, UUID> {

    List<ProviderContract> findByCommunityIdAndProviderIdOrderByCreatedAtDesc(UUID communityId, UUID providerId);

    Optional<ProviderContract> findByIdAndCommunityIdAndProviderId(UUID id, UUID communityId, UUID providerId);
}
