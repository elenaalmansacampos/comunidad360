package com.comunidad360.config;

import com.comunidad360.community.CommunityRepository;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.bootstrap.enabled=true",
        "app.bootstrap.email=bootstrap@example.com",
        "app.bootstrap.password=StrongBootstrap123!",
        "app.bootstrap.community-name=Bootstrap Community"
})
class BootstrapAdminInitializerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired AppUserRepository userRepository;
    @Autowired CommunityRepository communityRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MembershipRoleRepository membershipRoleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void bootstrapCreatesVerifiedAdminAndCommunity() {
        var user = userRepository.findByEmailIgnoreCase("bootstrap@example.com").orElseThrow();
        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(passwordEncoder.matches("StrongBootstrap123!", user.getPasswordHash())).isTrue();
        var community = communityRepository.findAll().stream()
                .filter(value -> value.getName().equals("Bootstrap Community"))
                .findFirst().orElseThrow();
        var membership = membershipRepository.findByCommunityIdAndUserId(community.getId(), user.getId())
                .orElseThrow();
        assertThat(membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()))
                .contains("COMMUNITY_ADMIN");
    }
}
