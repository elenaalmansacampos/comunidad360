package com.comunidad360.config;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap", name = "enabled", havingValue = "true")
public class BootstrapAdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;
    private final String communityName;

    public BootstrapAdminInitializer(AppUserRepository userRepository,
                                     CommunityRepository communityRepository,
                                     MembershipRepository membershipRepository,
                                     MembershipRoleRepository membershipRoleRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.bootstrap.email:}") String email,
                                     @Value("${app.bootstrap.password:}") String password,
                                     @Value("${app.bootstrap.community-name:Comunidad Principal}") String communityName) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        this.password = password == null ? "" : password;
        this.communityName = communityName;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Bootstrap enabled but email/password are missing");
        }
        if (password.length() < 12) {
            throw new IllegalStateException("Bootstrap password must have at least 12 characters");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Instant now = Instant.now();
        AppUser user = userRepository.save(new AppUser(
                UUID.randomUUID(), email, passwordEncoder.encode(password),
                "Administrador", "es", UserStatus.ACTIVE, now, now));
        user.setEmailVerifiedAt(now);
        userRepository.save(user);

        Community community = communityRepository.findAll().stream().findFirst().orElseGet(() ->
                communityRepository.save(new Community(UUID.randomUUID(), communityName,
                        "Europe/Madrid", "es", "EUR", CommunityStatus.ACTIVE, now, now)));

        Membership membership = membershipRepository.findByCommunityIdAndUserId(community.getId(), user.getId())
                .orElseGet(() -> membershipRepository.save(new Membership(UUID.randomUUID(),
                        community.getId(), user.getId(), MembershipStatus.ACTIVE, now, now, now)));
        if (membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()).isEmpty()) {
            membershipRoleRepository.save(new MembershipRole(UUID.randomUUID(), membership.getId(),
                    "COMMUNITY_ADMIN", now, user.getId()));
        }
        log.info("Bootstrap admin created for {} in community {}", email, community.getId());
    }
}
