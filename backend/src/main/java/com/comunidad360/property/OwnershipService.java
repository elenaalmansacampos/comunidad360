package com.comunidad360.property;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.dto.CreateOwnershipRequest;
import com.comunidad360.property.dto.OwnershipResponse;
import com.comunidad360.property.dto.UpdateOwnershipRequest;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class OwnershipService {

    private final PropertyOwnershipRepository ownershipRepository;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final UnitAccessService unitAccessService;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public OwnershipService(PropertyOwnershipRepository ownershipRepository,
                            MembershipRepository membershipRepository,
                            AppUserRepository userRepository,
                            UnitAccessService unitAccessService,
                            TenantAccessService tenantAccessService,
                            AuditService auditService) {
        this.ownershipRepository = ownershipRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.unitAccessService = unitAccessService;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OwnershipResponse> list(UUID communityId, UUID userId, UUID unitId) {
        unitAccessService.requireView(communityId, userId, unitId);
        return ownershipRepository.findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OwnershipResponse create(UUID communityId, UUID userId, UUID unitId, CreateOwnershipRequest request,
                                    HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        validateMember(communityId, request.userId());
        validateShare(request.sharePercentage());
        validateDates(request.validFrom(), request.validTo());

        LocalDate today = LocalDate.now();
        List<PropertyOwnership> existing = ownershipRepository
                .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId);
        boolean newActive = isActiveOn(request.validFrom(), request.validTo(), today);
        if (newActive) {
            boolean duplicate = existing.stream()
                    .anyMatch(ownership -> ownership.getUserId().equals(request.userId())
                            && ownership.isActiveOn(today));
            if (duplicate) {
                throw BusinessException.conflict(
                        "OWNERSHIP_ALREADY_ACTIVE", "errors.ownership.duplicateActive");
            }
            BigDecimal activeSum = sumActive(existing, today);
            if (activeSum.add(request.sharePercentage()).compareTo(new BigDecimal("100")) > 0) {
                throw BusinessException.unprocessable(
                        "OWNERSHIP_SUM_EXCEEDS", "errors.ownership.sumExceeds");
            }
        }

        Instant now = Instant.now();
        PropertyOwnership ownership = new PropertyOwnership(
                UUID.randomUUID(),
                communityId,
                unitId,
                request.userId(),
                request.ownershipType(),
                request.sharePercentage(),
                request.validFrom(),
                request.validTo(),
                userId,
                now,
                now
        );
        PropertyOwnership saved = ownershipRepository.saveAndFlush(ownership);
        OwnershipResponse response = toResponse(saved);
        auditService.record(communityId, userId, "USER", "OWNERSHIP_CREATED", "OWNERSHIP",
                saved.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public OwnershipResponse update(UUID communityId, UUID userId, UUID unitId, UUID ownershipId,
                                    UpdateOwnershipRequest request, HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        PropertyOwnership ownership = find(communityId, unitId, ownershipId);
        if (request.version() != null && request.version() != ownership.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        OwnershipResponse before = toResponse(ownership);
        if (request.ownershipType() != null) {
            ownership.setOwnershipType(request.ownershipType());
        }
        if (request.sharePercentage() != null) {
            validateShare(request.sharePercentage());
            ownership.setSharePercentage(request.sharePercentage());
        }
        if (request.validFrom() != null) {
            ownership.setValidFrom(request.validFrom());
        }
        if (request.validTo() != null) {
            ownership.setValidTo(request.validTo());
        }
        validateDates(ownership.getValidFrom(), ownership.getValidTo());

        LocalDate today = LocalDate.now();
        if (ownership.isActiveOn(today)) {
            List<PropertyOwnership> others = ownershipRepository
                    .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                    .filter(other -> !other.getId().equals(ownershipId))
                    .toList();
            boolean duplicate = others.stream()
                    .anyMatch(other -> other.getUserId().equals(ownership.getUserId())
                            && other.isActiveOn(today));
            if (duplicate) {
                throw BusinessException.conflict(
                        "OWNERSHIP_ALREADY_ACTIVE", "errors.ownership.duplicateActive");
            }
            BigDecimal activeSum = sumActive(others, today);
            if (activeSum.add(ownership.getSharePercentage()).compareTo(new BigDecimal("100")) > 0) {
                throw BusinessException.unprocessable(
                        "OWNERSHIP_SUM_EXCEEDS", "errors.ownership.sumExceeds");
            }
        }

        ownership.setUpdatedAt(Instant.now());
        PropertyOwnership saved = ownershipRepository.saveAndFlush(ownership);
        OwnershipResponse after = toResponse(saved);
        auditService.record(communityId, userId, "USER", "OWNERSHIP_UPDATED", "OWNERSHIP",
                ownershipId, before, after, httpRequest);
        return after;
    }

    @Transactional
    public void end(UUID communityId, UUID userId, UUID unitId, UUID ownershipId,
                    HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "OWNERSHIP_MANAGE");
        unitAccessService.requireUnit(communityId, unitId);
        PropertyOwnership ownership = find(communityId, unitId, ownershipId);
        OwnershipResponse before = toResponse(ownership);
        if (ownership.getValidTo() == null) {
            LocalDate today = LocalDate.now();
            ownership.setValidTo(today.isAfter(ownership.getValidFrom())
                    ? today.minusDays(1)
                    : ownership.getValidFrom());
            ownership.setUpdatedAt(Instant.now());
            PropertyOwnership saved = ownershipRepository.saveAndFlush(ownership);
            auditService.record(communityId, userId, "USER", "OWNERSHIP_ENDED", "OWNERSHIP",
                    ownershipId, before, toResponse(saved), httpRequest);
        }
    }

    private PropertyOwnership find(UUID communityId, UUID unitId, UUID ownershipId) {
        return ownershipRepository.findByIdAndCommunityIdAndUnitId(ownershipId, communityId, unitId)
                .orElseThrow(() -> BusinessException.notFound(
                        "OWNERSHIP_NOT_FOUND", "errors.ownership.notFound"));
    }

    private OwnershipResponse toResponse(PropertyOwnership ownership) {
        AppUser user = userRepository.findById(ownership.getUserId()).orElse(null);
        return new OwnershipResponse(
                ownership.getId(),
                ownership.getUserId(),
                user == null ? null : user.getFullName(),
                user == null ? null : user.getEmail(),
                ownership.getOwnershipType(),
                ownership.getSharePercentage(),
                ownership.getValidFrom(),
                ownership.getValidTo(),
                ownership.isActiveOn(LocalDate.now()),
                ownership.getVersion()
        );
    }

    private void validateMember(UUID communityId, UUID userId) {
        membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(membership -> membership.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unprocessable(
                        "OWNERSHIP_USER_NOT_MEMBER", "errors.ownership.userNotMember"));
    }

    private void validateShare(BigDecimal sharePercentage) {
        if (sharePercentage == null
                || sharePercentage.compareTo(BigDecimal.ZERO) <= 0
                || sharePercentage.compareTo(new BigDecimal("100")) > 0) {
            throw BusinessException.unprocessable(
                    "OWNERSHIP_SHARE_INVALID", "errors.ownership.shareInvalid");
        }
    }

    private void validateDates(LocalDate validFrom, LocalDate validTo) {
        if (validFrom == null || (validTo != null && validTo.isBefore(validFrom))) {
            throw BusinessException.unprocessable(
                    "OWNERSHIP_DATES_INVALID", "errors.ownership.datesInvalid");
        }
    }

    private boolean isActiveOn(LocalDate validFrom, LocalDate validTo, LocalDate date) {
        return !validFrom.isAfter(date) && (validTo == null || validTo.isAfter(date));
    }

    private BigDecimal sumActive(List<PropertyOwnership> ownerships, LocalDate date) {
        return ownerships.stream()
                .filter(ownership -> ownership.isActiveOn(date))
                .map(PropertyOwnership::getSharePercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
