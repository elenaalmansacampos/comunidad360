package com.comunidad360.membership;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class MembershipPermissionId implements Serializable {

    private UUID membershipId;
    private String permissionCode;

    public MembershipPermissionId() {
    }

    public MembershipPermissionId(UUID membershipId, String permissionCode) {
        this.membershipId = membershipId;
        this.permissionCode = permissionCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MembershipPermissionId that)) {
            return false;
        }
        return Objects.equals(membershipId, that.membershipId)
                && Objects.equals(permissionCode, that.permissionCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(membershipId, permissionCode);
    }
}
