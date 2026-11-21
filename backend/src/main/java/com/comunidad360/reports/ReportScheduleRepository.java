package com.comunidad360.reports;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {

    List<ReportSchedule> findByCommunityIdOrderByCreatedAtAsc(UUID communityId);

    Optional<ReportSchedule> findByIdAndCommunityId(UUID id, UUID communityId);

    @Query("select schedule.id from ReportSchedule schedule "
            + "where schedule.enabled = true and schedule.nextRunAt <= :now")
    List<UUID> findDueIds(@Param("now") Instant now);
}
