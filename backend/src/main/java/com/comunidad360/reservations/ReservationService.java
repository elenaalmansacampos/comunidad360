package com.comunidad360.reservations;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.reservations.dto.CreateReservationRequest;
import com.comunidad360.reservations.dto.ReservationResponse;
import com.comunidad360.reservations.dto.UpdateReservationStatusRequest;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ReservationService {

    private static final List<ReservationStatus> BLOCKING_STATUSES =
            List.of(ReservationStatus.PENDING, ReservationStatus.APPROVED);

    private final AreaReservationRepository repository;
    private final CommonAreaRepository areaRepository;
    private final AppUserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public ReservationService(AreaReservationRepository repository, CommonAreaRepository areaRepository,
                              AppUserRepository userRepository,
                              TenantAccessService tenantAccessService, PermissionService permissionService,
                              AuditService auditService) {
        this.repository = repository;
        this.areaRepository = areaRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> list(UUID communityId, UUID userId, boolean mine, UUID areaId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "RESERVATION_VIEW");
        List<AreaReservation> reservations = mine
                ? repository.findByCommunityIdAndMembershipIdOrderByReservationDateDescStartTimeDesc(
                        communityId, membership.getId())
                : repository.findByCommunityIdOrderByReservationDateAscStartTimeAsc(communityId);
        return reservations.stream()
                .filter(reservation -> areaId == null || areaId.equals(reservation.getAreaId()))
                .map(reservation -> response(reservation, membership.getId()))
                .toList();
    }

    @Transactional
    public ReservationResponse create(UUID communityId, UUID userId, CreateReservationRequest request,
                                      HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "RESERVATION_CREATE");
        CommonArea area = areaRepository.findByIdAndCommunityId(request.areaId(), communityId)
                .filter(CommonArea::isActive)
                .orElseThrow(() -> BusinessException.notFound("COMMON_AREA_NOT_FOUND", "errors.resource.notFound"));
        LocalDate today = LocalDate.now();
        if (request.date().isBefore(today)) {
            throw BusinessException.unprocessable("RESERVATION_DATE_INVALID", "errors.reservation.dateInvalid");
        }
        if (!request.startTime().isBefore(request.endTime())) {
            throw BusinessException.unprocessable("RESERVATION_TIME_INVALID", "errors.reservation.timeInvalid");
        }
        if (request.date().equals(today) && request.startTime().isBefore(LocalTime.now())) {
            throw BusinessException.unprocessable("RESERVATION_TIME_INVALID", "errors.reservation.timeInvalid");
        }
        if (repository.existsOverlap(communityId, area.getId(), request.date(), request.startTime(),
                request.endTime(), BLOCKING_STATUSES)) {
            throw BusinessException.conflict("RESERVATION_CONFLICT", "errors.reservation.conflict");
        }
        Instant now = Instant.now();
        ReservationStatus status = area.isRequiresApproval() ? ReservationStatus.PENDING : ReservationStatus.APPROVED;
        AreaReservation reservation = repository.saveAndFlush(new AreaReservation(UUID.randomUUID(),
                communityId, area.getId(), membership.getId(), userId, request.date(), request.startTime(),
                request.endTime(), status, clean(request.notes()), now, now));
        ReservationResponse response = response(reservation, membership.getId());
        auditService.record(communityId, userId, "USER", "RESERVATION_CREATED", "RESERVATION",
                reservation.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public ReservationResponse review(UUID communityId, UUID userId, UUID reservationId,
                                      UpdateReservationStatusRequest request, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "RESERVATION_MANAGE");
        AreaReservation reservation = find(communityId, reservationId);
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw BusinessException.unprocessable("RESERVATION_STATUS_INVALID", "errors.reservation.statusInvalid");
        }
        if (request.status() != ReservationStatus.APPROVED && request.status() != ReservationStatus.REJECTED) {
            throw BusinessException.unprocessable("RESERVATION_STATUS_INVALID", "errors.reservation.statusInvalid");
        }
        ReservationResponse before = response(reservation, membership.getId());
        Instant now = Instant.now();
        reservation.setStatus(request.status());
        reservation.setReviewedByUserId(userId);
        reservation.setReviewedAt(now);
        reservation.setUpdatedAt(now);
        AreaReservation saved = repository.saveAndFlush(reservation);
        ReservationResponse after = response(saved, membership.getId());
        auditService.record(communityId, userId, "USER", "RESERVATION_REVIEWED", "RESERVATION",
                reservationId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void cancel(UUID communityId, UUID userId, UUID reservationId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "RESERVATION_VIEW");
        AreaReservation reservation = find(communityId, reservationId);
        boolean manage = permissionService.hasPermission(membership, "RESERVATION_MANAGE");
        if (!manage && !membership.getId().equals(reservation.getMembershipId())) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.APPROVED) {
            throw BusinessException.unprocessable("RESERVATION_STATUS_INVALID", "errors.reservation.statusInvalid");
        }
        ReservationResponse before = response(reservation, membership.getId());
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedAt(Instant.now());
        AreaReservation saved = repository.saveAndFlush(reservation);
        auditService.record(communityId, userId, "USER", "RESERVATION_CANCELLED", "RESERVATION",
                reservationId, before, response(saved, membership.getId()), httpRequest);
    }

    private AreaReservation find(UUID communityId, UUID reservationId) {
        return repository.findByIdAndCommunityId(reservationId, communityId)
                .orElseThrow(() -> BusinessException.notFound("RESERVATION_NOT_FOUND", "errors.resource.notFound"));
    }

    private ReservationResponse response(AreaReservation reservation, UUID currentMembershipId) {
        String areaName = areaRepository.findByIdAndCommunityId(reservation.getAreaId(), reservation.getCommunityId())
                .map(CommonArea::getName).orElse(null);
        return ReservationResponse.from(reservation, areaName,
                userName(reservation.getUserId()), userName(reservation.getReviewedByUserId()),
                reservation.getMembershipId().equals(currentMembershipId));
    }

    private String userName(UUID userId) {
        if (userId == null) {
            return null;
        }
        return userRepository.findById(userId).map(this::name).orElse(null);
    }

    private String name(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
