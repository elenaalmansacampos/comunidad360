package com.comunidad360.push;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<PushSubscription> findByEndpoint(String endpoint);
}
