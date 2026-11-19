package com.comunidad360.membership;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantAccessService {

    private final MembershipRepository membershipRepository;
    private final CommunityRepository communityRepository;
    private final PermissionService permissionService;

    public TenantAccessService(MembershipRepository membershipRepository,
                               CommunityRepository communityRepository,
                               PermissionService permissionService) {
        this.membershipRepository = membershipRepository;
        this.communityRepository = communityRepository;
        this.permissionService = permissionService;
    }

    @Transactional(readOnly = true)
    public Membership requireActiveMembership(UUID communityId, UUID userId) {
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, userId)
                .filter(value -> value.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.forbidden(
                        "TENANT_FORBIDDEN", "errors.tenant.forbidden"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.forbidden(
                        "TENANT_FORBIDDEN", "errors.tenant.forbidden"));
        if (community.getStatus() != CommunityStatus.ACTIVE) {
            throw BusinessException.forbidden("TENANT_FORBIDDEN", "errors.tenant.forbidden");
        }
        return membership;
    }

    @Transactional(readOnly = true)
    public Membership requirePermission(UUID communityId, UUID userId, String permissionCode) {
        Membership membership = requireActiveMembership(communityId, userId);
        permissionService.require(membership, permissionCode);
        return membership;
    }
}
