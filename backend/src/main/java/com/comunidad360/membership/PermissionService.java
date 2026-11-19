package com.comunidad360.membership;

import com.comunidad360.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PermissionService {

    private final MembershipRoleRepository membershipRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MembershipPermissionRepository membershipPermissionRepository;

    public PermissionService(MembershipRoleRepository membershipRoleRepository,
                             RolePermissionRepository rolePermissionRepository,
                             MembershipPermissionRepository membershipPermissionRepository) {
        this.membershipRoleRepository = membershipRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.membershipPermissionRepository = membershipPermissionRepository;
    }

    @Transactional(readOnly = true)
    public Set<String> permissionsFor(Membership membership) {
        List<String> roleCodes = membershipRoleRepository.findRoleCodesByMembershipId(membership.getId());
        Set<String> permissions = new HashSet<>();
        if (!roleCodes.isEmpty()) {
            permissions.addAll(rolePermissionRepository.findGrantedPermissions(roleCodes));
        }
        for (MembershipPermission override : membershipPermissionRepository.findByMembershipId(membership.getId())) {
            if (override.isGranted()) {
                permissions.add(override.getPermissionCode());
            } else {
                permissions.remove(override.getPermissionCode());
            }
        }
        return permissions;
    }

    @Transactional(readOnly = true)
    public boolean hasPermission(Membership membership, String permissionCode) {
        return permissionsFor(membership).contains(permissionCode);
    }

    @Transactional(readOnly = true)
    public void require(Membership membership, String permissionCode) {
        if (!hasPermission(membership, permissionCode)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
    }
}
