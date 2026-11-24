package com.comunidad360.directory;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.directory.dto.DirectoryEntryResponse;
import com.comunidad360.directory.dto.UpdateDirectoryProfileRequest;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.property.PropertyOwnership;
import com.comunidad360.property.PropertyOwnershipRepository;
import com.comunidad360.property.Tenancy;
import com.comunidad360.property.TenancyRepository;
import com.comunidad360.property.TenancyStatus;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DirectoryService {

    private final NeighborProfileRepository profileRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final AppUserRepository userRepository;
    private final PropertyOwnershipRepository ownershipRepository;
    private final TenancyRepository tenancyRepository;
    private final UnitRepository unitRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AuditService auditService;

    public DirectoryService(NeighborProfileRepository profileRepository,
                            MembershipRepository membershipRepository,
                            MembershipRoleRepository membershipRoleRepository,
                            AppUserRepository userRepository,
                            PropertyOwnershipRepository ownershipRepository,
                            TenancyRepository tenancyRepository,
                            UnitRepository unitRepository,
                            TenantAccessService tenantAccessService,
                            PermissionService permissionService,
                            AuditService auditService) {
        this.profileRepository = profileRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.userRepository = userRepository;
        this.ownershipRepository = ownershipRepository;
        this.tenancyRepository = tenancyRepository;
        this.unitRepository = unitRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<DirectoryEntryResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "DIRECTORY_VIEW");
        Map<UUID, NeighborProfile> profiles = new HashMap<>();
        profileRepository.findByCommunityId(communityId)
                .forEach(profile -> profiles.put(profile.getMembershipId(), profile));
        return membershipRepository.findByCommunityIdOrderByCreatedAtAsc(communityId).stream()
                .filter(member -> member.getStatus() == MembershipStatus.ACTIVE)
                .map(member -> entry(communityId, member, profiles.get(member.getId()), userId))
                .sorted(Comparator.comparing(DirectoryEntryResponse::fullName,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public DirectoryEntryResponse updateMe(UUID communityId, UUID userId,
                                           UpdateDirectoryProfileRequest request,
                                           HttpServletRequest httpRequest) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "DIRECTORY_VIEW");
        NeighborProfile profile = profileRepository.findByMembershipId(membership.getId()).orElse(null);
        Instant now = Instant.now();
        if (profile == null) {
            profile = new NeighborProfile(UUID.randomUUID(), communityId, membership.getId(), userId,
                    clean(request.phone()), request.showEmail() == null || request.showEmail(),
                    request.showPhone() != null && request.showPhone(), now, now);
        } else {
            profile.setPhone(clean(request.phone()));
            if (request.showEmail() != null) profile.setShowEmail(request.showEmail());
            if (request.showPhone() != null) profile.setShowPhone(request.showPhone());
            profile.setUpdatedAt(now);
        }
        NeighborProfile saved = profileRepository.saveAndFlush(profile);
        DirectoryEntryResponse response = entry(communityId, membership, saved, userId);
        auditService.record(communityId, userId, "USER", "DIRECTORY_PROFILE_UPDATED", "MEMBERSHIP",
                membership.getId(), null, response, httpRequest);
        return response;
    }

    private DirectoryEntryResponse entry(UUID communityId, Membership membership,
                                         NeighborProfile profile, UUID currentUserId) {
        AppUser user = userRepository.findById(membership.getUserId()).orElse(null);
        List<String> roles = membershipRoleRepository.findRoleCodesByMembershipId(membership.getId());
        List<String> units = unitCodes(communityId, membership.getUserId());
        boolean self = membership.getUserId().equals(currentUserId);
        boolean showEmail = profile == null || profile.isShowEmail();
        boolean showPhone = profile != null && profile.isShowPhone();
        String email = user == null ? null : (self || showEmail ? user.getEmail() : null);
        String phone = profile == null ? null : (self || showPhone ? profile.getPhone() : null);
        String name = user == null ? null
                : (user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName());
        return new DirectoryEntryResponse(membership.getId(), membership.getUserId(), name, email, phone,
                roles, units, showEmail, showPhone, self);
    }

    private List<String> unitCodes(UUID communityId, UUID userId) {
        LocalDate today = LocalDate.now();
        Set<UUID> unitIds = new HashSet<>();
        for (PropertyOwnership ownership : ownershipRepository
                .findByCommunityIdAndUserIdOrderByValidFromDesc(communityId, userId)) {
            if (ownership.isActiveOn(today)) unitIds.add(ownership.getUnitId());
        }
        for (Tenancy tenancy : tenancyRepository
                .findByCommunityIdAndUserIdAndStatus(communityId, userId, TenancyStatus.ACTIVE)) {
            if (tenancy.isActiveOn(today)) unitIds.add(tenancy.getUnitId());
        }
        List<String> codes = new ArrayList<>();
        for (UUID unitId : unitIds) {
            unitRepository.findById(unitId).map(Unit::getCode).ifPresent(codes::add);
        }
        codes.sort(String.CASE_INSENSITIVE_ORDER);
        return codes;
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
