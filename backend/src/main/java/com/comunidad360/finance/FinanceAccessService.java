package com.comunidad360.finance;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.PropertyOwnershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FinanceAccessService {

    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final PropertyOwnershipRepository ownershipRepository;

    public FinanceAccessService(TenantAccessService tenantAccessService,
                                PermissionService permissionService,
                                PropertyOwnershipRepository ownershipRepository) {
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.ownershipRepository = ownershipRepository;
    }

    @Transactional(readOnly = true)
    public Membership requireFinanceView(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "FINANCE_VIEW");
        return membership;
    }

    @Transactional(readOnly = true)
    public Membership requireStaff(UUID communityId, UUID userId, String permission) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        if (!isStaff(membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        permissionService.require(membership, permission);
        return membership;
    }

    @Transactional(readOnly = true)
    public boolean isStaff(Membership membership) {
        return permissionService.hasPermission(membership, "FINANCE_MANAGE")
                || permissionService.hasPermission(membership, "MEMBER_VIEW")
                || permissionService.hasPermission(membership, "REPORT_VIEW");
    }

    @Transactional(readOnly = true)
    public Set<UUID> ownedUnitIds(UUID communityId, UUID userId) {
        LocalDate today = LocalDate.now();
        return ownershipRepository.findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId).stream()
                .filter(ownership -> ownership.isActiveOn(today))
                .map(ownership -> ownership.getUnitId())
                .collect(Collectors.toSet());
    }
}
