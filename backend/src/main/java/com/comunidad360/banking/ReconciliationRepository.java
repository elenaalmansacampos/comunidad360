package com.comunidad360.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, UUID> {

    List<Reconciliation> findByCommunityId(UUID communityId);

    Optional<Reconciliation> findByIdAndCommunityId(UUID id, UUID communityId);

    Optional<Reconciliation> findByCommunityIdAndBankTransactionId(UUID communityId, UUID bankTransactionId);

    boolean existsByCommunityIdAndPaymentId(UUID communityId, UUID paymentId);

    boolean existsByCommunityIdAndLedgerEntryId(UUID communityId, UUID ledgerEntryId);
}
