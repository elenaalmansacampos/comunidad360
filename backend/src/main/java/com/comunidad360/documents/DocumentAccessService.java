package com.comunidad360.documents;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.PropertyOwnershipRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentAccessService {

    private static final Set<String> BOARD_ROLES = Set.of(
            "COMMUNITY_ADMIN", "PRESIDENT", "SECRETARY", "TREASURER"
    );

    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final MembershipRoleRepository membershipRoleRepository;
    private final DocumentAclRepository aclRepository;
    private final PropertyOwnershipRepository ownershipRepository;

    public DocumentAccessService(TenantAccessService tenantAccessService,
                                 PermissionService permissionService,
                                 MembershipRoleRepository membershipRoleRepository,
                                 DocumentAclRepository aclRepository,
                                 PropertyOwnershipRepository ownershipRepository) {
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.membershipRoleRepository = membershipRoleRepository;
        this.aclRepository = aclRepository;
        this.ownershipRepository = ownershipRepository;
    }

    @Transactional(readOnly = true)
    public Membership requireMembership(UUID communityId, UUID userId) {
        return tenantAccessService.requireActiveMembership(communityId, userId);
    }

    @Transactional(readOnly = true)
    public void requireViewPermission(Membership membership) {
        permissionService.require(membership, "DOCUMENT_VIEW");
    }

    @Transactional(readOnly = true)
    public void requireUpload(Membership membership) {
        if (!permissionService.hasPermission(membership, "DOCUMENT_UPLOAD")
                && !permissionService.hasPermission(membership, "DOCUMENT_MANAGE")) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
    }

    @Transactional(readOnly = true)
    public void requireManage(Membership membership) {
        permissionService.require(membership, "DOCUMENT_MANAGE");
    }

    @Transactional(readOnly = true)
    public boolean canView(Document document, UUID userId, Membership membership) {
        if (permissionService.hasPermission(membership, "DOCUMENT_MANAGE")) {
            return true;
        }
        if (!permissionService.hasPermission(membership, "DOCUMENT_VIEW")) {
            return false;
        }
        if (userId.equals(document.getOwnerUserId())) {
            return true;
        }
        List<String> roles = membershipRoleRepository.findRoleCodesByMembershipId(membership.getId());
        boolean board = roles.stream().anyMatch(BOARD_ROLES::contains);
        if (hasAclAccess(document, userId, membership, roles)) {
            return true;
        }
        return switch (document.getVisibility()) {
            case COMMUNITY -> true;
            case BOARD -> board;
            case OWNERS -> board || hasActiveOwnership(document.getCommunityId(), userId);
            case ROLE, PRIVATE -> false;
        };
    }

    @Transactional(readOnly = true)
    public void requireView(Document document, UUID userId, Membership membership) {
        if (!canView(document, userId, membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
    }

    private boolean hasAclAccess(Document document, UUID userId, Membership membership, List<String> roles) {
        return aclRepository.findByCommunityIdAndDocumentId(document.getCommunityId(), document.getId())
                .stream()
                .anyMatch(acl -> switch (acl.getPrincipalType()) {
                    case USER -> acl.getPrincipalId().equals(userId.toString());
                    case MEMBERSHIP -> acl.getPrincipalId().equals(membership.getId().toString());
                    case ROLE -> roles.contains(acl.getPrincipalId());
                });
    }

    private boolean hasActiveOwnership(UUID communityId, UUID userId) {
        LocalDate today = LocalDate.now();
        return ownershipRepository.findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId).stream()
                .anyMatch(ownership -> ownership.isActiveOn(today));
    }
}
