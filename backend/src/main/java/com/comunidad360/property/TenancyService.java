package com.comunidad360.property;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.dto.CreateTenancyRequest;
import com.comunidad360.property.dto.TenancyResponse;
import com.comunidad360.property.dto.UpdateTenancyRequest;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class TenancyService {

    private final TenancyRepository tenancyRepository;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final UnitAccessService unitAccessService;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public TenancyService(TenancyRepository tenancyRepository,
                          MembershipRepository membershipRepository,
                          AppUserRepository userRepository,
                          UnitAccessService unitAccessService,
                          TenantAccessService tenantAccessService,
                          AuditService auditService) {
        this.tenancyRepository = tenancyRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.unitAccessService = unitAccessService;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<TenancyResponse> list(UUID communityId, UUID userId, UUID unitId) {
        unitAccessService.requireView(communityId, userId, unitId);
        return tenancyRepository.findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TenancyResponse create(UUID communityId, UUID userId, UUID unitId, CreateTenancyRequest request,
                                  HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        validateMember(communityId, request.userId());
        validateDates(request.validFrom(), request.validTo());

        LocalDate today = LocalDate.now();
        List<Tenancy> existing = tenancyRepository
                .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId);
        boolean duplicate = existing.stream()
                .anyMatch(tenancy -> tenancy.getUserId().equals(request.userId())
                        && tenancy.isActiveOn(today));
        if (duplicate) {
            throw BusinessException.conflict(
                    "TENANCY_ALREADY_ACTIVE", "errors.tenancy.duplicateActive");
        }

        Instant now = Instant.now();
        Tenancy tenancy = new Tenancy(
                UUID.randomUUID(),
                communityId,
                unitId,
                request.userId(),
                request.validFrom(),
                request.validTo(),
                TenancyStatus.ACTIVE,
                userId,
                now,
                now
        );
        Tenancy saved = tenancyRepository.saveAndFlush(tenancy);
        TenancyResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "TENANCY_CREATED", "TENANCY",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public TenancyResponse update(UUID communityId, UUID userId, UUID unitId, UUID tenancyId,
                                  UpdateTenancyRequest request, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        Tenancy tenancy = find(communityId, unitId, tenancyId);
        if (request.version() != null && request.version() != tenancy.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        TenancyResponse before = toResponse(tenancy);
        if (request.validFrom() != null) {
            tenancy.setValidFrom(request.validFrom());
        }
        if (request.validTo() != null) {
            tenancy.setValidTo(request.validTo());
        }
        if (request.status() != null) {
            tenancy.setStatus(request.status());
        }
        validateDates(tenancy.getValidFrom(), tenancy.getValidTo());

        LocalDate today = LocalDate.now();
        if (tenancy.getStatus() == TenancyStatus.ACTIVE && tenancy.isActiveOn(today)) {
            boolean duplicate = tenancyRepository
                    .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                    .filter(other -> !other.getId().equals(tenancyId))
                    .anyMatch(other -> other.getUserId().equals(tenancy.getUserId())
                            && other.isActiveOn(today));
            if (duplicate) {
                throw BusinessException.conflict(
                        "TENANCY_ALREADY_ACTIVE", "errors.tenancy.duplicateActive");
            }
        }

        tenancy.setUpdatedAt(Instant.now());
        Tenancy saved = tenancyRepository.saveAndFlush(tenancy);
        TenancyResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "TENANCY_UPDATED", "TENANCY",
                tenancyId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void revoke(UUID communityId, UUID userId, UUID unitId, UUID tenancyId,
                       HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        Tenancy tenancy = find(communityId, unitId, tenancyId);
        TenancyResponse before = toResponse(tenancy);
        if (tenancy.getStatus() == TenancyStatus.ACTIVE) {
            LocalDate today = LocalDate.now();
            LocalDate endDate = today.isAfter(tenancy.getValidFrom())
                    ? today.minusDays(1)
                    : tenancy.getValidFrom();
            tenancy.setStatus(TenancyStatus.REVOKED);
            tenancy.setValidTo(endDate);
            tenancy.setUpdatedAt(Instant.now());
            Tenancy saved = tenancyRepository.saveAndFlush(tenancy);
            auditService.record(communityId, userId, "USER", "TENANCY_REVOKED", "TENANCY",
                    tenancyId, before, toResponse(saved), httpRequest);
        }
    }

    private Tenancy find(UUID communityId, UUID unitId, UUID tenancyId) {
        return tenancyRepository.findByIdAndCommunityIdAndUnitId(tenancyId, communityId, unitId)
                .orElseThrow(() -> BusinessException.notFound(
                        "TENANCY_NOT_FOUND", "errors.tenancy.notFound"));
    }

    private TenancyResponse toResponse(Tenancy tenancy) {
        AppUser user = userRepository.findById(tenancy.getUserId()).orElse(null);
        return new TenancyResponse(
                tenancy.getId(),
                tenancy.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                tenancy.getValidFrom(),
                tenancy.getValidTo(),
                tenancy.getStatus(),
                tenancy.isActiveOn(LocalDate.now()),
                tenancy.getVersion()
        );
    }

    private void validateMember(UUID communityId, UUID userId) {
        membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "TENANCY_USER_NOT_MEMBER", "errors.tenancy.userNotMember"));
    }

    private void validateDates(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null || (validTo != null && validTo.isBefore(validFrom))) {
            throw BusinessException.unprocessable(
                    "TENANCY_DATES_INVALID", "errors.tenancy.datesInvalid");
        }
    }
}
