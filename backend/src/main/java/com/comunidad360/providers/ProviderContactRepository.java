package com.comunidad360.providers;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProviderContactRepository extends JpaRepository<ProviderContact, UUID> {
    List<ProviderContact> findByCommunityIdAndProviderIdOrderByPrimaryContactDescNameAsc(
            UUID communityId, UUID providerId);
    List<ProviderContact> findByCommunityIdAndProviderId(UUID communityId, UUID providerId);
}
