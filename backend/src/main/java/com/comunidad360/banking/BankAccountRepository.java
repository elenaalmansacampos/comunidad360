package com.comunidad360.banking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {

    List<BankAccount> findByCommunityIdOrderByNameAsc(UUID communityId);

    Optional<BankAccount> findByIdAndCommunityId(UUID id, UUID communityId);
}
