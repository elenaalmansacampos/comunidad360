package com.comunidad360.providers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProviderRepository extends JpaRepository<Provider, UUID> {
    List<Provider> findByCommunityIdOrderByNameAsc(UUID communityId);
    Optional<Provider> findByIdAndCommunityId(UUID id, UUID communityId);
}
