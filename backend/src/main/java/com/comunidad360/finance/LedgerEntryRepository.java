package com.comunidad360.finance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByCommunityIdOrderByEntryDateDesc(UUID communityId);

    List<LedgerEntry> findByCommunityIdAndFiscalYearIdOrderByEntryDateDesc(UUID communityId, UUID fiscalYearId);

    List<LedgerEntry> findByCommunityIdAndFiscalYearIdAndStatusOrderByCategoryAsc(
            UUID communityId, UUID fiscalYearId, LedgerEntryStatus status);

    Optional<LedgerEntry> findByIdAndCommunityId(UUID id, UUID communityId);

    @Query("""
            select coalesce(max(l.entryNumber), 0)
            from LedgerEntry l
            where l.communityId = :communityId and l.fiscalYearId = :fiscalYearId
            """)
    Long maxEntryNumber(@Param("communityId") UUID communityId, @Param("fiscalYearId") UUID fiscalYearId);

    @Query("""
            select coalesce(sum(l.amount), 0)
            from LedgerEntry l
            where l.communityId = :communityId
              and l.fiscalYearId = :fiscalYearId
              and l.status = :status
              and l.type = :type
            """)
    BigDecimal sumByType(@Param("communityId") UUID communityId,
                         @Param("fiscalYearId") UUID fiscalYearId,
                         @Param("status") LedgerEntryStatus status,
                         @Param("type") LedgerEntryType type);

    long countByCommunityIdAndFiscalYearIdAndStatus(UUID communityId, UUID fiscalYearId,
                                                    LedgerEntryStatus status);

    @Query(value = """
            select extract(month from entry_date)::int as month,
                   type,
                   coalesce(sum(amount), 0)
              from ledger_entry
             where community_id = :communityId
               and fiscal_year_id = :fiscalYearId
               and status = 'POSTED'
             group by 1, 2
             order by 1
            """, nativeQuery = true)
    List<Object[]> sumMonthly(@Param("communityId") UUID communityId,
                              @Param("fiscalYearId") UUID fiscalYearId);
}
