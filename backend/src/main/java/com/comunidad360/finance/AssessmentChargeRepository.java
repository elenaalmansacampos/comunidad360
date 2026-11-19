package com.comunidad360.finance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentChargeRepository extends JpaRepository<AssessmentCharge, UUID> {

    List<AssessmentCharge> findByCommunityIdAndAssessmentIdOrderByUnitIdAsc(UUID communityId, UUID assessmentId);

    @Query("""
            select c from AssessmentCharge c, Assessment a
            where c.assessmentId = a.id
              and c.communityId = :communityId
              and a.fiscalYearId = :fiscalYearId
            order by a.dueDate desc, c.id
            """)
    List<AssessmentCharge> findForFiscalYear(@Param("communityId") UUID communityId,
                                             @Param("fiscalYearId") UUID fiscalYearId);

    @Query("""
            select c from AssessmentCharge c, Assessment a
            where c.assessmentId = a.id
              and c.communityId = :communityId
              and a.fiscalYearId = :fiscalYearId
              and c.unitId in :unitIds
            order by a.dueDate desc, c.id
            """)
    List<AssessmentCharge> findForFiscalYearAndUnits(@Param("communityId") UUID communityId,
                                                     @Param("fiscalYearId") UUID fiscalYearId,
                                                     @Param("unitIds") Collection<UUID> unitIds);

    Optional<AssessmentCharge> findByIdAndCommunityId(UUID id, UUID communityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AssessmentCharge c where c.id = :id and c.communityId = :communityId")
    Optional<AssessmentCharge> findForUpdate(@Param("id") UUID id, @Param("communityId") UUID communityId);

    @Query("""
            select coalesce(sum(c.amount - c.paidAmount), 0)
            from AssessmentCharge c, Assessment a
            where c.assessmentId = a.id
              and c.communityId = :communityId
              and a.fiscalYearId = :fiscalYearId
              and c.status in :statuses
            """)
    BigDecimal sumPending(@Param("communityId") UUID communityId,
                          @Param("fiscalYearId") UUID fiscalYearId,
                          @Param("statuses") Collection<ChargeStatus> statuses);

    @Query("""
            select coalesce(sum(c.amount - c.paidAmount), 0)
            from AssessmentCharge c, Assessment a
            where c.assessmentId = a.id
              and c.communityId = :communityId
              and a.fiscalYearId = :fiscalYearId
              and c.unitId in :unitIds
              and c.status in :statuses
            """)
    BigDecimal sumPendingForUnits(@Param("communityId") UUID communityId,
                                  @Param("fiscalYearId") UUID fiscalYearId,
                                  @Param("unitIds") Collection<UUID> unitIds,
                                  @Param("statuses") Collection<ChargeStatus> statuses);

    long countByAssessmentId(UUID assessmentId);
}
