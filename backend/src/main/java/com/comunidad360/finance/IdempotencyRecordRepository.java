package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByCommunityIdAndUserIdAndOperationAndIdempotencyKey(
            UUID communityId, UUID userId, String operation, String idempotencyKey);
}
