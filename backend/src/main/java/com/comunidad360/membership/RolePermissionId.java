package com.comunidad360.membership;

import java.io.Serializable;
import java.util.Objects;

public class RolePermissionId implements Serializable {

    private String roleCode;
    private String permissionCode;

    public RolePermissionId() {
    }

    public RolePermissionId(String roleCode, String permissionCode) {
        this.roleCode = roleCode;
        this.permissionCode = permissionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RolePermissionId that)) {
            return false;
        }
        return Objects.equals(roleCode, that.roleCode) && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleCode, permissionCode);
    }
}
