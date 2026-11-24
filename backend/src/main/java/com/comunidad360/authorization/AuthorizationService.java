package com.comunidad360.authorization;

import com.comunidad360.audit.AuditService;
import com.comunidad360.authorization.dto.AuthorizationResponse;
import com.comunidad360.authorization.dto.CreateAuthorizationRequest;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final AuthorizationRepository repository;
    private final MembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public AuthorizationService(AuthorizationRepository repository,
                                MembershipRepository membershipRepository,
                                AppUserRepository userRepository,
                                TenantAccessService tenantAccessService,
                                PermissionService permissionService,
                                AuditService auditService) {
        this.repository = repository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AuthorizationResponse> list(UUID communityId, UUID userId, String direction, boolean all) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "AUTHORIZATION_VIEW");
        boolean canManage = permissionService.hasPermission(membership, "AUTHORIZATION_MANAGE");
        List<Authorization> authorizations;
        if (all && canManage) {
            authorizations = repository.findByCommunityIdOrderByCreatedAtDesc(communityId);
        } else if ("GRANTED".equalsIgnoreCase(direction)) {
            authorizations = repository.findByCommunityIdAndGrantorMembershipIdOrderByCreatedAtDesc(
                    communityId, membership.getId());
        } else if ("RECEIVED".equalsIgnoreCase(direction)) {
            authorizations = repository.findByCommunityIdAndGranteeMembershipIdOrderByCreatedAtDesc(
                    communityId, membership.getId());
        } else {
            Map<UUID, Authorization> merged = new LinkedHashMap<>();
            repository.findByCommunityIdAndGrantorMembershipIdOrderByCreatedAtDesc(communityId, membership.getId())
                    .forEach(authorization -> merged.put(authorization.getId(), authorization));
            repository.findByCommunityIdAndGranteeMembershipIdOrderByCreatedAtDesc(communityId, membership.getId())
                    .forEach(authorization -> merged.putIfAbsent(authorization.getId(), authorization));
            authorizations = new ArrayList<>(merged.values());
            authorizations.sort((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()));
        }
        return toResponses(authorizations);
    }

    @Transactional
    public AuthorizationResponse create(UUID communityId, UUID userId, CreateAuthorizationRequest request,
                                        HttpServletRequest httpRequest) {
        Membership grantor = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(grantor, "AUTHORIZATION_CREATE");
        Membership grantee = membershipRepository.findById(request.granteeMembershipId())
                .filter(member -> member.getCommunityId().equals(communityId))
                .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.unprocessable("AUTHORIZATION_GRANTEE_INVALID",
                        "errors.authorization.granteeInvalid"));
        if (grantor.getId().equals(grantee.getId())) {
            throw BusinessException.unprocessable("AUTHORIZATION_SELF", "errors.authorization.self");
        }
        AuthorizationScope scope = request.scope();
        if (!permissionService.hasPermission(grantor, scope.permission())) {
            throw BusinessException.unprocessable("AUTHORIZATION_SCOPE_NOT_ALLOWED",
                    "errors.authorization.scopeNotAllowed");
        }
        LocalDate validFrom = request.validFrom() == null ? LocalDate.now() : request.validFrom();
        if (request.validUntil() != null && request.validUntil().isBefore(validFrom)) {
            throw BusinessException.unprocessable("AUTHORIZATION_DATES_INVALID",
                    "errors.authorization.datesInvalid");
        }
        Instant now = Instant.now();
        Optional<Authorization> existing = repository
                .findFirstByCommunityIdAndGrantorMembershipIdAndGranteeMembershipIdAndScopeAndStatusOrderByCreatedAtDesc(
                        communityId, grantor.getId(), grantee.getId(), scope, AuthorizationStatus.ACTIVE);
        Authorization authorization;
        String action;
        if (existing.isPresent()) {
            authorization = existing.get();
            authorization.setValidFrom(validFrom);
            authorization.setValidUntil(request.validUntil());
            authorization.setNotes(clean(request.notes()));
            authorization.setUpdatedAt(now);
            authorization.setRevokedAt(null);
            authorization = repository.saveAndFlush(authorization);
            action = "AUTHORIZATION_UPDATED";
        } else {
            authorization = repository.saveAndFlush(new Authorization(UUID.randomUUID(), communityId,
                    grantor.getId(), grantee.getId(), scope, AuthorizationStatus.ACTIVE, validFrom,
                    request.validUntil(), clean(request.notes()), userId, now, now));
            action = "AUTHORIZATION_CREATED";
        }
        AuthorizationResponse response = toResponse(authorization);
        auditService.record(communityId, userId, "USER", action, "AUTHORIZATION",
                authorization.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public void revoke(UUID communityId, UUID userId, UUID authorizationId, HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "AUTHORIZATION_VIEW");
        Authorization authorization = repository.findByIdAndCommunityId(authorizationId, communityId)
                .orElseThrow(() -> BusinessException.notFound("AUTHORIZATION_NOT_FOUND",
                        "errors.authorization.notFound"));
        boolean manage = permissionService.hasPermission(membership, "AUTHORIZATION_MANAGE");
        if (!manage && !membership.getId().equals(authorization.getGrantorMembershipId())) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        if (authorization.getStatus() != AuthorizationStatus.ACTIVE) {
            throw BusinessException.unprocessable("AUTHORIZATION_NOT_ACTIVE", "errors.authorization.notActive");
        }
        AuthorizationResponse before = toResponse(authorization);
        Instant now = Instant.now();
        authorization.setStatus(AuthorizationStatus.REVOKED);
        authorization.setRevokedAt(now);
        authorization.setUpdatedAt(now);
        Authorization saved = repository.saveAndFlush(authorization);
        auditService.record(communityId, userId, "USER", "AUTHORIZATION_REVOKED", "AUTHORIZATION",
                authorizationId, before, toResponse(saved), httpRequest);
    }

    @Transactional(readOnly = true)
    public Membership resolveOnBehalf(UUID communityId, Membership actor, UUID onBehalfOfMembershipId,
                                      AuthorizationScope scope) {
        if (onBehalfOfMembershipId == null || onBehalfOfMembershipId.equals(actor.getId())) {
            return actor;
        }
        Membership grantor = membershipRepository.findById(onBehalfOfMembershipId)
                .filter(member -> member.getCommunityId().equals(communityId))
                .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.forbidden("AUTHORIZATION_GRANTOR_INVALID",
                        "errors.authorization.grantorInvalid"));
        Authorization authorization = repository
                .findFirstByCommunityIdAndGrantorMembershipIdAndGranteeMembershipIdAndScopeAndStatusOrderByCreatedAtDesc(
                        communityId, grantor.getId(), actor.getId(), scope, AuthorizationStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.forbidden("AUTHORIZATION_NOT_FOUND",
                        "errors.authorization.notFound"));
        if (!authorization.isEffective(LocalDate.now())) {
            throw BusinessException.forbidden("AUTHORIZATION_EXPIRED", "errors.authorization.expired");
        }
        if (!permissionService.hasPermission(grantor, scope.permission())) {
            throw BusinessException.forbidden("AUTHORIZATION_SCOPE_NOT_ALLOWED",
                    "errors.authorization.scopeNotAllowed");
        }
        return grantor;
    }

    @Transactional(readOnly = true)
    public List<Authorization> receivedEffective(UUID communityId, UUID granteeMembershipId,
                                                 AuthorizationScope scope) {
        return repository.findByCommunityIdAndGranteeMembershipIdOrderByCreatedAtDesc(communityId, granteeMembershipId)
                .stream()
                .filter(authorization -> authorization.getScope() == scope)
                .filter(authorization -> authorization.getStatus() == AuthorizationStatus.ACTIVE)
                .filter(authorization -> authorization.isEffective(LocalDate.now()))
                .toList();
    }

    public Object auditAfter(UUID actorUserId, Membership effective, Object result) {
        if (effective.getUserId().equals(actorUserId)) {
            return result;
        }
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("result", result);
        context.put("onBehalfOfMembershipId", effective.getId());
        context.put("onBehalfOfUserId", effective.getUserId());
        return context;
    }

    private AuthorizationResponse toResponse(Authorization authorization) {
        return toResponses(List.of(authorization)).get(0);
    }

    private List<AuthorizationResponse> toResponses(List<Authorization> authorizations) {
        Map<UUID, MemberInfo> members = memberInfo(authorizations);
        return authorizations.stream().map(authorization -> {
            MemberInfo grantor = members.get(authorization.getGrantorMembershipId());
            MemberInfo grantee = members.get(authorization.getGranteeMembershipId());
            return AuthorizationResponse.from(authorization,
                    grantor == null ? null : grantor.name(),
                    grantee == null ? null : grantee.name(),
                    grantor == null ? null : grantor.userId(),
                    grantee == null ? null : grantee.userId(),
                    authorization.isEffective(LocalDate.now()));
        }).toList();
    }

    private Map<UUID, MemberInfo> memberInfo(Collection<Authorization> authorizations) {
        Set<UUID> membershipIds = new HashSet<>();
        authorizations.forEach(authorization -> {
            membershipIds.add(authorization.getGrantorMembershipId());
            membershipIds.add(authorization.getGranteeMembershipId());
        });
        List<Membership> memberships = membershipRepository.findAllById(membershipIds);
        Set<UUID> userIds = new HashSet<>();
        memberships.forEach(membership -> userIds.add(membership.getUserId()));
        Map<UUID, String> names = new LinkedHashMap<>();
        userRepository.findAllById(userIds).forEach(user -> names.put(user.getId(), name(user)));
        Map<UUID, MemberInfo> result = new LinkedHashMap<>();
        memberships.forEach(membership -> result.put(membership.getId(),
                new MemberInfo(membership.getUserId(), names.get(membership.getUserId()))));
        return result;
    }

    private String name(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record MemberInfo(UUID userId, String name) {
    }
}
