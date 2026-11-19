package com.comunidad360.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankImportBatchRepository extends JpaRepository<BankImportBatch, UUID> {

    List<BankImportBatch> findByCommunityIdOrderByImportedAtDesc(UUID communityId);
}
