package com.comunidad360.vecino;

import com.comunidad360.audit.AuditService;
import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import com.comunidad360.vecino.dto.VecinoAccessResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class VecinoAccessService {

    private static final String ROLE = "VECINO";
    private static final char[] PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private static final int PASSWORD_LENGTH = 16;

    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantAccessService tenantAccessService;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public VecinoAccessService(CommunityRepository communityRepository,
                               MembershipRepository membershipRepository,
                               MembershipRoleRepository membershipRoleRepository,
                               AppUserRepository userRepository,
                               PasswordEncoder passwordEncoder,
                               TenantAccessService tenantAccessService,
                               AuditService auditService) {
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantAccessService = tenantAccessService;
        this.auditService = auditService;
    }

    @Transactional
    public VecinoAccessResponse generate(UUID communityId, UUID adminUserId, HttpServletRequest request) {
        tenantAccessService.requirePermission(communityId, adminUserId, "COMMUNITY_MANAGE");
        communityRepository.findById(communityId)
                .orElseThrow(() -> BusinessException.notFound("COMMUNITY_NOT_FOUND", "errors.community.notFound"));
        String email = "vecino-" + communityId.toString().substring(0, 8) + "@comunidad360.local";
        String password = randomPassword();
        Instant now = Instant.now();
        AppUser user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode(password), "Vecino",
                    "es", UserStatus.ACTIVE, now, now);
            user.setEmailVerifiedAt(now);
            userRepository.save(user);
        } else {
            user.setPasswordHash(passwordEncoder.encode(password));
            user.setFullName("Vecino");
            user.setStatus(UserStatus.ACTIVE);
            user.setEmailVerifiedAt(now);
            user.setUpdatedAt(now);
            userRepository.save(user);
        }
        final AppUser vecino = user;
        Membership membership = membershipRepository.findByCommunityIdAndUserId(communityId, vecino.getId())
                .orElseGet(() -> new Membership(UUID.randomUUID(), communityId, vecino.getId(),
                        MembershipStatus.ACTIVE, now, now, now));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            membership.setStatus(MembershipStatus.ACTIVE);
            membership.setUpdatedAt(now);
        }
        membershipRepository.save(membership);
        if (!membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()).contains(ROLE)) {
            membershipRoleRepository.save(new MembershipRole(UUID.randomUUID(), membership.getId(),
                    ROLE, now, adminUserId));
        }
        auditService.record(communityId, adminUserId, "USER", "VECINO_ACCESS_GENERATED", "MEMBERSHIP",
                membership.getId(), null, Map.of("email", email, "role", ROLE), request);
        return new VecinoAccessResponse(email, password, ROLE);
    }

    private String randomPassword() {
        StringBuilder builder = new StringBuilder(PASSWORD_LENGTH);
        for (int index = 0; index < PASSWORD_LENGTH; index++) {
            builder.append(PASSWORD_ALPHABET[random.nextInt(PASSWORD_ALPHABET.length)]);
        }
        return builder.toString();
    }
}
