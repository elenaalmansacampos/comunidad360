package com.comunidad360.membership;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "membership_role")
public class MembershipRole {

    @Id
    private UUID id;

    @Column(name = "membership_id", nullable = false)
    private UUID membershipId;

    @Column(name = "role_code", nullable = false, length = 40)
    private String roleCode;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    @Column(name = "assigned_by_user_id")
    private UUID assignedByUserId;

    protected MembershipRole() {
    }

    public MembershipRole(UUID id, UUID membershipId, String roleCode, Instant assignedAt, UUID assignedByUserId) {
        this.id = id;
        this.membershipId = membershipId;
        this.roleCode = roleCode;
        this.assignedAt = assignedAt;
        this.assignedByUserId = assignedByUserId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMembershipId() {
        return membershipId;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public UUID getAssignedByUserId() {
        return assignedByUserId;
    }
}
