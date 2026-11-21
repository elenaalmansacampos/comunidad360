package com.comunidad360.organization;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.organization.dto.*;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrganizationService {

    private static final String COMMUNITY_ADMIN_ROLE = "COMMUNITY_ADMIN";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository memberRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final AppUserRepository userRepository;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;

    public OrganizationService(OrganizationRepository organizationRepository,
                               OrganizationMemberRepository memberRepository,
                               CommunityRepository communityRepository,
                               MembershipRepository membershipRepository,
                               MembershipRoleRepository membershipRoleRepository,
                               AppUserRepository userRepository,
                               TenantAccessService tenantAccessService,
                               AuditService auditService) {
        this.organizationRepository = organizationRepository;
        this.memberRepository = memberRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.userRepository = userRepository;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<OrganizationResponse> mine(UUID userId) {
        List<OrganizationResponse> result = new ArrayList<>();
        for (OrganizationMember member : memberRepository
                .findByUserIdAndStatusOrderByCreatedAtAsc(userId, OrganizationMemberStatus.ACTIVE)) {
            organizationRepository.findById(member.getOrganizationId()).ifPresent(organization ->
                    result.add(response(organization, member.getRole())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse get(UUID organizationId, UUID userId) {
        Access access = requireMember(organizationId, userId);
        return response(access.organization(), access.member().getRole());
    }

    @Transactional
    public OrganizationResponse create(UUID userId, CreateOrganizationRequest request,
                                       HttpServletRequest httpRequest) {
        Instant now = Instant.now();
        Organization organization = organizationRepository.saveAndFlush(new Organization(
                UUID.randomUUID(), request.name().trim(), clean(request.taxId()),
                OrganizationStatus.ACTIVE, userId, now, now));
        memberRepository.saveAndFlush(new OrganizationMember(
                UUID.randomUUID(), organization.getId(), userId, OrganizationMemberRole.ORG_ADMIN,
                OrganizationMemberStatus.ACTIVE, now, now));
        OrganizationResponse response = response(organization, OrganizationMemberRole.ORG_ADMIN);
        auditService.record(null, userId, "USER", "ORGANIZATION_CREATED", "ORGANIZATION",
                organization.getId(), null, response, httpRequest);
        return response;
    }

    @Transactional
    public OrganizationResponse update(UUID organizationId, UUID userId, UpdateOrganizationRequest request,
                                       HttpServletRequest httpRequest) {
        Access access = requireAdmin(organizationId, userId);
        Organization organization = access.organization();
        OrganizationResponse before = response(organization, access.member().getRole());
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            organization.setName(request.name().trim());
        }
        if (request.taxId() != null) {
            organization.setTaxId(clean(request.taxId()));
        }
        if (request.status() != null) {
            organization.setStatus(request.status());
        }
        organization.setUpdatedAt(Instant.now());
        Organization saved = organizationRepository.saveAndFlush(organization);
        OrganizationResponse response = response(saved, access.member().getRole());
        auditService.record(null, userId, "USER", "ORGANIZATION_UPDATED", "ORGANIZATION",
                organizationId, before, response, httpRequest);
        return response;
    }

    @Transactional(readOnly = true)
    public List<OrganizationCommunityResponse> communities(UUID organizationId, UUID userId) {
        requireMember(organizationId, userId);
        return communityRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(community -> new OrganizationCommunityResponse(
                        community.getId(), community.getName(), community.getStatus().name()))
                .toList();
    }

    @Transactional
    public void attachCommunity(UUID organizationId, UUID communityId, UUID userId,
                                HttpServletRequest httpRequest) {
        requireAdmin(organizationId, userId);
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound("COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        if (community.getOrganizationId() != null && !community.getOrganizationId().equals(organizationId)) {
            throw BusinessException.conflict("COMMUNITY_ALREADY_IN_ORGANIZATION", "errors.conflict");
        }
        community.setOrganizationId(organizationId);
        community.setUpdatedAt(Instant.now());
        communityRepository.save(community);
        materializeForCommunity(organizationId, communityId, userId);
        auditService.record(communityId, userId, "USER", "ORGANIZATION_COMMUNITY_ATTACHED", "ORGANIZATION",
                organizationId, null, Map.of("communityId", communityId), httpRequest);
    }

    @Transactional
    public void detachCommunity(UUID organizationId, UUID communityId, UUID userId,
                                HttpServletRequest httpRequest) {
        requireAdmin(organizationId, userId);
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound("COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        if (!organizationId.equals(community.getOrganizationId())) {
            throw BusinessException.notFound("ORGANIZATION_COMMUNITY_NOT_FOUND", "errors.resource.notFound");
        }
        community.setOrganizationId(null);
        community.setUpdatedAt(Instant.now());
        communityRepository.save(community);
        removeMaterializedForCommunity(organizationId, communityId);
        auditService.record(communityId, userId, "USER", "ORGANIZATION_COMMUNITY_DETACHED", "ORGANIZATION",
                organizationId, Map.of("communityId", communityId), null, httpRequest);
    }

    @Transactional(readOnly = true)
    public List<OrganizationMemberResponse> members(UUID organizationId, UUID userId) {
        requireMember(organizationId, userId);
        List<OrganizationMemberResponse> result = new ArrayList<>();
        for (OrganizationMember member : memberRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId)) {
            AppUser user = userRepository.findById(member.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            result.add(new OrganizationMemberResponse(user.getId(), user.getFullName(), user.getEmail(),
                    member.getRole(), member.getStatus(), member.getCreatedAt()));
        }
        return result;
    }

    @Transactional
    public OrganizationMemberResponse addMember(UUID organizationId, UUID userId,
                                                AddOrganizationMemberRequest request,
                                                HttpServletRequest httpRequest) {
        requireAdmin(organizationId, userId);
        AppUser target = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "errors.resource.notFound"));
        Instant now = Instant.now();
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, target.getId())
                .orElseGet(() -> new OrganizationMember(UUID.randomUUID(), organizationId, target.getId(),
                        request.role(), OrganizationMemberStatus.ACTIVE, now, now));
        member.setRole(request.role());
        member.setStatus(OrganizationMemberStatus.ACTIVE);
        member.setUpdatedAt(now);
        OrganizationMember saved = memberRepository.saveAndFlush(member);
        if (saved.getRole() == OrganizationMemberRole.ORG_ADMIN) {
            for (Community community : communityRepository.findByOrganizationIdOrderByNameAsc(organizationId)) {
                ensureCommunityAdmin(community.getId(), target.getId(), organizationId, userId, now);
            }
        }
        auditService.record(null, userId, "USER", "ORGANIZATION_MEMBER_ADDED", "ORGANIZATION",
                organizationId, null, Map.of("userId", target.getId(), "role", saved.getRole().name()), httpRequest);
        return new OrganizationMemberResponse(target.getId(), target.getFullName(), target.getEmail(),
                saved.getRole(), saved.getStatus(), saved.getCreatedAt());
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID targetUserId, UUID userId,
                             HttpServletRequest httpRequest) {
        Access access = requireAdmin(organizationId, userId);
        OrganizationMember target = memberRepository.findByOrganizationIdAndUserId(organizationId, targetUserId)
                .orElseThrow(() -> BusinessException.notFound("ORGANIZATION_MEMBER_NOT_FOUND",
                        "errors.resource.notFound"));
        if (target.getRole() == OrganizationMemberRole.ORG_ADMIN
                && target.getStatus() == OrganizationMemberStatus.ACTIVE
                && memberRepository.countByOrganizationIdAndRoleAndStatus(organizationId,
                        OrganizationMemberRole.ORG_ADMIN, OrganizationMemberStatus.ACTIVE) <= 1) {
            throw BusinessException.unprocessable("LAST_ORG_ADMIN", "errors.validation");
        }
        target.setStatus(OrganizationMemberStatus.INACTIVE);
        target.setUpdatedAt(Instant.now());
        memberRepository.save(target);
        Instant now = Instant.now();
        for (Community community : communityRepository.findByOrganizationIdOrderByNameAsc(organizationId)) {
            removeMaterialized(community.getId(), targetUserId, organizationId, now);
        }
        auditService.record(null, userId, "USER", "ORGANIZATION_MEMBER_REMOVED", "ORGANIZATION",
                organizationId, Map.of("userId", targetUserId), null, httpRequest);
    }

    private OrganizationResponse response(Organization organization, OrganizationMemberRole role) {
        long communities = communityRepository.findByOrganizationIdOrderByNameAsc(organization.getId()).size();
        return new OrganizationResponse(organization.getId(), organization.getName(), organization.getTaxId(),
                organization.getStatus(), role, communities, organization.getCreatedAt());
    }

    private Access requireMember(UUID organizationId, UUID userId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> BusinessException.notFound("ORGANIZATION_NOT_FOUND", "errors.resource.notFound"));
        OrganizationMember member = memberRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .filter(value -> value.getStatus() == OrganizationMemberStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.forbidden("ORGANIZATION_FORBIDDEN", "errors.permission.denied"));
        return new Access(organization, member);
    }

    private Access requireAdmin(UUID organizationId, UUID userId) {
        Access access = requireMember(organizationId, userId);
        if (access.member().getRole() != OrganizationMemberRole.ORG_ADMIN) {
            throw BusinessException.forbidden("ORGANIZATION_ADMIN_REQUIRED", "errors.permission.denied");
        }
        return access;
    }

    private void materializeForCommunity(UUID organizationId, UUID communityId, UUID actorUserId) {
        Instant now = Instant.now();
        for (OrganizationMember member : memberRepository.findByOrganizationIdOrderByCreatedAtAsc(organizationId)) {
            if (member.getStatus() == OrganizationMemberStatus.ACTIVE
                    && member.getRole() == OrganizationMemberRole.ORG_ADMIN) {
                ensureCommunityAdmin(communityId, member.getUserId(), organizationId, actorUserId, now);
            }
        }
    }

    private void ensureCommunityAdmin(UUID communityId, UUID targetUserId, UUID organizationId,
                                      UUID actorUserId, Instant now) {
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, targetUserId)
                .orElse(null);
        if (membership == null) {
            membership = membershipRepository.saveAndFlush(new Membership(UUID.randomUUID(), communityId,
                    targetUserId, MembershipStatus.ACTIVE, now, now, now));
            membership.setSourceOrganizationId(organizationId);
            membershipRepository.save(membership);
        } else if (!organizationId.equals(membership.getSourceOrganizationId())) {
            return;
        } else if (membership.getStatus() != MembershipStatus.ACTIVE) {
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setUpdatedAt(now);
            membershipRepository.save(membership);
        }
        if (!membershipRoleRepository.findRoleCodesByMembershipId(membership.getId())
                .contains(COMMUNITY_ADMIN_ROLE)) {
            membershipRoleRepository.save(new MembershipRole(UUID.randomUUID(), membership.getId(),
                    COMMUNITY_ADMIN_ROLE, now, actorUserId));
        }
    }

    private void removeMaterializedForCommunity(UUID organizationId, UUID communityId) {
        Instant now = Instant.now();
        for (Membership membership : membershipRepository.findBySourceOrganizationId(organizationId)) {
            if (membership.getCommunityId().equals(communityId)) {
                membership.setStatus(MembershipStatus.REMOVED);
                membership.setUpdatedAt(now);
                membershipRepository.save(membership);
            }
        }
    }

    private void removeMaterialized(UUID communityId, UUID targetUserId, UUID organizationId, Instant now) {
        membershipRepository.findByCommunityIdAndUserId(communityId, targetUserId)
                .filter(membership -> organizationId.equals(membership.getSourceOrganizationId()))
                .ifPresent(membership -> {
                    membership.setStatus(MembershipStatus.REMOVED);
                    membership.setUpdatedAt(now);
                    membershipRepository.save(membership);
                });
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record Access(Organization organization, OrganizationMember member) {
    }
}
