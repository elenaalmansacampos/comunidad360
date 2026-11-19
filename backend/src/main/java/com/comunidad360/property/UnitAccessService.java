package com.comunidad360.property;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class UnitAccessService {

    private final UnitRepository unitRepository;
    private final PropertyOwnershipRepository ownershipRepository;
    private final TenancyRepository tenancyRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;

    public UnitAccessService(UnitRepository unitRepository,
                             PropertyOwnershipRepository ownershipRepository,
                             TenancyRepository tenancyRepository,
                             TenantAccessService tenantAccessService,
                             PermissionService permissionService) {
        this.unitRepository = unitRepository;
        this.ownershipRepository = ownershipRepository;
        this.tenancyRepository = tenancyRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public Unit requireUnit(UUID communityId, UUID unitId) {
        return unitRepository.findByIdAndCommunityId(unitId, communityId)
                .filter(unit -> unit.getArchivedAt() == null)
                .orElseThrow(() -> BusinessException.notFound("UNIT_NOT_FOUND", "errors.unit.notFound"));
    }

    @Transactional(readOnly = true)
    public Unit requireView(UUID communityId, UUID userId, UUID unitId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        Unit unit = requireUnit(communityId, unitId);
        if (permissionService.hasPermission(membership, "OWNERSHIP_MANAGE")
                || permissionService.hasPermission(membership, "MEMBER_VIEW")) {
            return unit;
        }
        LocalDate today = LocalDate.now();
        boolean owner = ownershipRepository
                .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                .anyMatch(ownership -> ownership.getUserId().equals(userId) && ownership.isActiveOn(today));
        boolean tenant = tenancyRepository
                .findByCommunityIdAndUnitIdOrderByValidFromDesc(communityId, unitId).stream()
                .anyMatch(tenancy -> tenancy.getUserId().equals(userId) && tenancy.isActiveOn(today));
        if (!owner && !tenant) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        return unit;
    }
}
