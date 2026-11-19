package com.comunidad360.community;

import com.comunidad360.audit.AuditService;
import com.comunidad360.auth.AuthService;
import com.comunidad360.auth.Invitation;
import com.comunidad360.auth.InvitationRepository;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.dto.CommunityResponse;
import com.comunidad360.community.dto.CommunitySummaryResponse;
import com.comunidad360.community.dto.CreateInvitationRequest;
import com.comunidad360.community.dto.InvitationResponse;
import com.comunidad360.community.dto.MemberResponse;
import com.comunidad360.community.dto.UpdateCommunityRequest;
import com.comunidad360.config.AppProperties;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CommunityService {

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final AppUserRepository userRepository;
    private final InvitationRepository invitationRepository;
    private final TenantAccessService tenantAccessService;
    private final AuthService authService;
    private final AuditService auditService;
    private final AppProperties properties;

    public CommunityService(CommunityRepository communityRepository,
                            MembershipRepository membershipRepository,
                            MembershipRoleRepository membershipRoleRepository,
                            AppUserRepository userRepository,
                            InvitationRepository invitationRepository,
                            TenantAccessService tenantAccessService,
                            AuthService authService,
                            AuditService auditService,
                            AppProperties properties) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.userRepository = userRepository;
        this.invitationRepository = invitationRepository;
        this.tenantAccessService = tenantAccessService;
        this.authService = authService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<CommunitySummaryResponse> listForUser(UUID userId) {
        List<CommunitySummaryResponse> result = new ArrayList<>();
        for (Membership membership : membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)) {
            communityRepository.findById(membership.getCommunityId()).ifPresent(community ->
                    result.add(toSummary(community, membership)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public CommunityResponse get(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_VIEW");
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        return CommunityResponse.from(community);
    }

    @Transactional
    public CommunityResponse update(UUID communityId, UUID userId, UpdateCommunityRequest request,
                                    HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "COMMUNITY_MANAGE");
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound(
                        "COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        if (request.version() != null && request.version() != community.getVersion()) {
            throw BusinessException.conflict("VERSION_CONFLICT", "errors.conflict");
        }
        CommunityResponse before = CommunityResponse.from(community);
        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            community.setName(request.name().trim());
        }
        if (request.taxId() != null) {
            community.setTaxId(request.taxId());
        }
        if (request.address() != null) {
            community.setAddress(request.address());
        }
        if (request.city() != null) {
            community.setCity(request.city());
        }
        if (request.province() != null) {
            community.setProvince(request.province());
        }
        if (request.postalCode() != null) {
            community.setPostalCode(request.postalCode());
        }
        if (request.timezone() != null) {
            try {
                ZoneId.of(request.timezone());
            } catch (DateTimeException exception) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            community.setTimezone(request.timezone());
        }
        if (request.defaultLanguage() != null) {
            String language = request.defaultLanguage().trim().toLowerCase(Locale.ROOT);
            boolean supported = properties.getSupportedLanguages().stream()
                    .anyMatch(value -> value.equalsIgnoreCase(language));
            if (!supported) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            community.setDefaultLanguage(language);
        }
        community.setUpdatedAt(Instant.now());
        Community saved = communityRepository.saveAndFlush(community);
        CommunityResponse after = CommunityResponse.from(saved);
        auditService.record(communityId, userId, "USER", "COMMUNITY_UPDATED", "COMMUNITY", communityId,
                before, after, httpRequest);
        return after;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> members(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "MEMBER_VIEW");
        List<MemberResponse> result = new ArrayList<>();
        for (Membership membership : membershipRepository.findByCommunityIdOrderByCreatedAtAsc(communityId)) {
            AppUser user = userRepository.findById(membership.getUserId()).orElse(null);
            if (user == null) {
                continue;
            }
            result.add(new MemberResponse(
                    membership.getId(),
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    membership.getStatus().name(),
                    membershipRoleRepository.findRoleCodesByMembershipId(membership.getId())
            ));
        }
        return result;
    }

    @Transactional
    public InvitationResponse invite(UUID communityId, UUID userId, CreateInvitationRequest request,
                                     HttpServletRequest httpRequest) {
        tenantAccessService.requirePermission(communityId, userId, "MEMBER_INVITE");
        AuthService.CreatedInvitation created = authService.createInvitation(
                communityId, userId, request.email(), request.role());
        Invitation invitation = created.invitation();
        auditService.record(communityId, userId, "USER", "INVITATION_CREATED", "INVITATION",
                invitation.getId(), null,
                Map.of("email", invitation.getEmail(), "role", invitation.getRoleCode()), httpRequest);
        return toResponse(invitation, created.rawToken());
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> invitations(UUID communityId, UUID userId) {
        tenantAccessService.requirePermission(communityId, userId, "MEMBER_VIEW");
        return invitationRepository.findByCommunityIdOrderByCreatedAtDesc(communityId).stream()
                .map(invitation -> toResponse(invitation, null))
                .toList();
    }

    private CommunitySummaryResponse toSummary(Community community, Membership membership) {
        return new CommunitySummaryResponse(
                community.getId(),
                community.getName(),
                membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()),
                community.getStatus().name()
        );
    }

    private InvitationResponse toResponse(Invitation invitation, String devToken) {
        String status;
        if (invitation.getAcceptedAt() != null) {
            status = "ACCEPTED";
        } else if (invitation.getExpiresAt().isBefore(Instant.now())) {
            status = "EXPIRED";
        } else {
            status = "PENDING";
        }
        String exposedToken = properties.getSecurity().isExposeDevTokens() ? devToken : null;
        return new InvitationResponse(
                invitation.getId(),
                invitation.getEmail(),
                invitation.getRoleCode(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt(),
                status,
                exposedToken
        );
    }
}
