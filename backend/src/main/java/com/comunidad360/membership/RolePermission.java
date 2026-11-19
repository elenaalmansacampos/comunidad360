package com.comunidad360.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_permission")
@IdClass(RolePermissionId.class)
public class RolePermission {

    @Id
    @Column(name = "role_code", length = 40)
    private String roleCode;

    @Id
    @Column(name = "permission_code", length = 60)
    private String permissionCode;

    @Column(nullable = false)
    private boolean granted;

    protected RolePermission() {
    }

    public String getRoleCode() {
        return roleCode;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public boolean isGranted() {
        return granted;
    }
}
