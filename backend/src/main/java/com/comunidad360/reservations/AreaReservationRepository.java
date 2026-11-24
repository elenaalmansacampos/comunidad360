package com.comunidad360.reservations;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AreaReservationRepository extends JpaRepository<AreaReservation, UUID> {

    List<AreaReservation> findByCommunityIdOrderByReservationDateAscStartTimeAsc(UUID communityId);

    List<AreaReservation> findByCommunityIdAndMembershipIdOrderByReservationDateDescStartTimeDesc(
            UUID communityId, UUID membershipId);

    List<AreaReservation> findByCommunityIdAndAreaIdOrderByReservationDateAscStartTimeAsc(
            UUID communityId, UUID areaId);

    Optional<AreaReservation> findByIdAndCommunityId(UUID id, UUID communityId);

    @Query("""
            select count(reservation) > 0 from AreaReservation reservation
            where reservation.communityId = :communityId
              and reservation.areaId = :areaId
              and reservation.reservationDate = :date
              and reservation.status in :statuses
              and reservation.startTime < :endTime
              and reservation.endTime > :startTime
            """)
    boolean existsOverlap(@Param("communityId") UUID communityId, @Param("areaId") UUID areaId,
                          @Param("date") LocalDate date, @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("statuses") List<ReservationStatus> statuses);
}
