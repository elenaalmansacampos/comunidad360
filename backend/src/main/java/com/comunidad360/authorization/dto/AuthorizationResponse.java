package com.comunidad360.authorization.dto;

import com.comunidad360.authorization.Authorization;
import com.comunidad360.authorization.AuthorizationScope;
import com.comunidad360.authorization.AuthorizationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AuthorizationResponse(
        UUID id, UUID grantorMembershipId, UUID grantorUserId, String grantorName,
        UUID granteeMembershipId, UUID granteeUserId, String granteeName,
        AuthorizationScope scope, AuthorizationStatus status, boolean effective,
        LocalDate validFrom, LocalDate validUntil, String notes,
        Instant createdAt, Instant revokedAt
) {
    public static AuthorizationResponse from(Authorization authorization, String grantorName,
                                             String granteeName, UUID grantorUserId,
                                             UUID granteeUserId, boolean effective) {
        return new AuthorizationResponse(authorization.getId(), authorization.getGrantorMembershipId(),
                grantorUserId, grantorName, authorization.getGranteeMembershipId(), granteeUserId,
                granteeName, authorization.getScope(), authorization.getStatus(), effective,
                authorization.getValidFrom(), authorization.getValidUntil(), authorization.getNotes(),
                authorization.getCreatedAt(), authorization.getRevokedAt());
    }
}
