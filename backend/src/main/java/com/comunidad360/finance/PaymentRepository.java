package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByCommunityIdOrderByPaymentDateDesc(UUID communityId);

    List<Payment> findByCommunityIdAndAssessmentChargeIdOrderByPaymentDateDesc(UUID communityId,
                                                                               UUID assessmentChargeId);

    Optional<Payment> findByIdAndCommunityId(UUID id, UUID communityId);
}
