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
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class DevDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipRoleRepository membershipRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;

    public DevDataInitializer(AppUserRepository userRepository,
                              CommunityRepository communityRepository,
                              MembershipRepository membershipRepository,
                              MembershipRoleRepository membershipRoleRepository,
                              PasswordEncoder passwordEncoder,
                              AppProperties properties) {
        this.userRepository = userRepository;
        this.communityRepository = communityRepository;
        this.membershipRepository = membershipRepository;
        this.membershipRoleRepository = membershipRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        String email = properties.getSeed().getAdminEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        Instant now = Instant.now();
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode(properties.getSeed().getAdminPassword()),
                "Administrador Demo",
                properties.getDefaultLanguage(),
                UserStatus.ACTIVE,
                now,
                now
        );
        user.setEmailVerifiedAt(now);
        userRepository.save(user);

        Community community = new Community(
                UUID.randomUUID(),
                properties.getSeed().getCommunityName(),
                "Europe/Madrid",
                properties.getDefaultLanguage(),
                "EUR",
                CommunityStatus.ACTIVE,
                now,
                now
        );
        communityRepository.save(community);

        Membership membership = new Membership(
                UUID.randomUUID(),
                community.getId(),
                user.getId(),
                MembershipStatus.ACTIVE,
                now,
                now,
                now
        );
        membershipRepository.save(membership);
        membershipRoleRepository.save(new MembershipRole(
                UUID.randomUUID(),
                membership.getId(),
                "COMMUNITY_ADMIN",
                now,
                user.getId()
        ));
        log.info("Dev seed created: user={} community={}", email, community.getName());
    }
}
