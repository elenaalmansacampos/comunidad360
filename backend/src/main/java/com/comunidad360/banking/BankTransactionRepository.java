package com.comunidad360.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID> {

    List<BankTransaction> findByCommunityIdOrderByValueDateDesc(UUID communityId);

    List<BankTransaction> findByCommunityIdAndBankAccountIdOrderByValueDateDesc(UUID communityId,
                                                                                UUID bankAccountId);

    Optional<BankTransaction> findByIdAndCommunityId(UUID id, UUID communityId);

    boolean existsByBankAccountIdAndRawHash(UUID bankAccountId, String rawHash);
}
