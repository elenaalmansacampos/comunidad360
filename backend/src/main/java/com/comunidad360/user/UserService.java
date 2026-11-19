package com.comunidad360.user;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.dto.CommunitySummaryResponse;
import com.comunidad360.config.AppProperties;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.user.dto.UpdateLanguageRequest;
import com.comunidad360.user.dto.UpdateProfileRequest;
import com.comunidad360.user.dto.UserResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final AppProperties properties;

    public UserService(AppUserRepository userRepository,
                       CommunityRepository communityRepository,
                       MembershipRepository membershipRepository,
                       MembershipRoleRepository membershipRoleRepository,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public UserResponse profile(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "errors.resource.notFound"));
        List<CommunitySummaryResponse> memberships = new ArrayList<>();
        for (Membership membership : membershipRepository.findByUserIdAndStatus(userId, MembershipStatus.ACTIVE)) {
            communityRepository.findById(membership.getCommunityId()).ifPresent(community ->
                    memberships.add(toSummary(community, membership)));
        }
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPreferredLanguage(),
                user.getStatus().name(),
                user.getEmailVerifiedAt(),
                memberships
        );
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "errors.resource.notFound"));
        if (request.fullName() != null) {
            if (request.fullName().isBlank()) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "errors.validation");
            }
            user.setFullName(request.fullName().trim());
        }
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return profile(userId);
    }

    @Transactional
    public UserResponse updateLanguage(UUID userId, UpdateLanguageRequest request) {
        String language = request.language().trim().toLowerCase(Locale.ROOT);
        boolean supported = properties.getSupportedLanguages().stream()
                .anyMatch(value -> value.equalsIgnoreCase(language));
        if (!supported) {
            throw BusinessException.unprocessable("UNSUPPORTED_LANGUAGE", "errors.validation");
        }
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("USER_NOT_FOUND", "errors.resource.notFound"));
        user.setPreferredLanguage(language);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        return profile(userId);
    }

    private CommunitySummaryResponse toSummary(Community community, Membership membership) {
        return new CommunitySummaryResponse(
                community.getId(),
                community.getName(),
                membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()),
                community.getStatus().name()
        );
    }
}
