package com.comunidad360.incidents;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.PropertyOwnershipRepository;
import com.comunidad360.property.TenancyRepository;
import com.comunidad360.property.TenancyStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class IncidentAccessService {

    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final PropertyOwnershipRepository ownershipRepository;
    private final TenancyRepository tenancyRepository;

    public IncidentAccessService(TenantAccessService tenantAccessService,
                                 PermissionService permissionService,
                                 PropertyOwnershipRepository ownershipRepository,
                                 TenancyRepository tenancyRepository) {
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.ownershipRepository = ownershipRepository;
        this.tenancyRepository = tenancyRepository;
    }

    @Transactional(readOnly = true)
    public Membership requireMembership(UUID communityId, UUID userId) {
        return tenantAccessService.requireActiveMembership(communityId, userId);
    }

    @Transactional(readOnly = true)
    public void requireCreate(UUID communityId, UUID userId) {
        Membership membership = requireMembership(communityId, userId);
        permissionService.require(membership, "INCIDENT_CREATE");
    }

    @Transactional(readOnly = true)
    public void requireCreate(Membership membership) {
        permissionService.require(membership, "INCIDENT_CREATE");
    }

    @Transactional(readOnly = true)
    public void requireManage(UUID communityId, UUID userId) {
        Membership membership = requireMembership(communityId, userId);
        permissionService.require(membership, "INCIDENT_MANAGE");
    }

    @Transactional(readOnly = true)
    public void requireComment(Membership membership) {
        permissionService.require(membership, "INCIDENT_COMMENT");
    }

    @Transactional(readOnly = true)
    public boolean canViewAll(Membership membership) {
        return permissionService.hasPermission(membership, "INCIDENT_VIEW_ALL");
    }

    @Transactional(readOnly = true)
    public boolean canViewAny(Membership membership) {
        return canViewAll(membership)
                || permissionService.hasPermission(membership, "INCIDENT_VIEW_OWN")
                || permissionService.hasPermission(membership, "INCIDENT_CREATE");
    }

    @Transactional(readOnly = true)
    public boolean canView(Incident incident, UUID userId, Membership membership) {
        if (canViewAll(membership)) {
            return true;
        }
        if (userId.equals(incident.getReportedByUserId())) {
            return true;
        }
        return incident.getUnitId() != null && accessibleUnitIds(incident.getCommunityId(), userId)
                .contains(incident.getUnitId());
    }

    @Transactional(readOnly = true)
    public void requireView(Incident incident, UUID userId, Membership membership) {
        if (!canView(incident, userId, membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
    }

    @Transactional(readOnly = true)
    public Set<UUID> accessibleUnitIds(UUID communityId, UUID userId) {
        Set<UUID> unitIds = new HashSet<>();
        LocalDate today = LocalDate.now();
        ownershipRepository.findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId).stream()
                .filter(ownership -> ownership.isActiveOn(today))
                .forEach(ownership -> unitIds.add(ownership.getUnitId()));
        tenancyRepository.findByCommunityIdAndUserIdAndStatus(communityId, userId, TenancyStatus.ACTIVE).stream()
                .filter(tenancy -> tenancy.isActiveOn(today))
                .forEach(tenancy -> unitIds.add(tenancy.getUnitId()));
        return unitIds;
    }
}
