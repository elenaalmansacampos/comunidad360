package com.comunidad360.providers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProviderNoteRepository extends JpaRepository<ProviderNote, UUID> {

    List<ProviderNote> findByCommunityIdAndProviderIdOrderByCreatedAtDesc(UUID communityId, UUID providerId);
}
