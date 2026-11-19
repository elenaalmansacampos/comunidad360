package com.comunidad360.finance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByCommunityIdOrderByIssueDateDesc(UUID communityId);

    List<Invoice> findByCommunityIdAndFiscalYearIdOrderByIssueDateDesc(UUID communityId, UUID fiscalYearId);

    Optional<Invoice> findByIdAndCommunityId(UUID id, UUID communityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Invoice i where i.id = :id and i.communityId = :communityId")
    Optional<Invoice> findForUpdate(@Param("id") UUID id, @Param("communityId") UUID communityId);

    boolean existsByCommunityIdAndProviderNameIgnoreCaseAndNumber(UUID communityId, String providerName,
                                                                  String number);

    @Query("""
            select coalesce(sum(i.total - i.paidAmount), 0)
            from Invoice i
            where i.communityId = :communityId
              and i.fiscalYearId = :fiscalYearId
              and i.status = :status
            """)
    BigDecimal sumPending(@Param("communityId") UUID communityId,
                          @Param("fiscalYearId") UUID fiscalYearId,
                          @Param("status") InvoiceStatus status);
}
