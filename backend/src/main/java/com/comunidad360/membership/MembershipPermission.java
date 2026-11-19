package com.comunidad360.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_permission")
@IdClass(MembershipPermissionId.class)
public class MembershipPermission {

    @Id
    @Column(name = "membership_id")
    private UUID membershipId;

    @Id
    @Column(name = "permission_code", length = 60)
    private String permissionCode;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "granted_by_user_id")
    private UUID grantedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MembershipPermission() {
    }

    public MembershipPermission(UUID membershipId, String permissionCode, boolean granted,
                                UUID grantedByUserId, Instant createdAt) {
        this.membershipId = membershipId;
        this.permissionCode = permissionCode;
        this.granted = granted;
        this.grantedByUserId = grantedByUserId;
        this.createdAt = createdAt;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public boolean isGranted() {
        return granted;
    }
}
