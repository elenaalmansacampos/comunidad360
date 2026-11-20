package com.comunidad360.config;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminInitializerResetTest {

    private static final String EMAIL = "admin@example.com";
    private static final String NEW_PASSWORD = "NewPassword123!";

    private final AppUserRepository userRepository = mock(AppUserRepository.class);
    private final CommunityRepository communityRepository = mock(CommunityRepository.class);
    private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
    private final MembershipRoleRepository membershipRoleRepository = mock(MembershipRoleRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void resetsExistingAdminPasswordAndUnlocksAccount() {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), EMAIL, "old-hash", "Administrador", "es",
                UserStatus.SUSPENDED, now, now);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(now.plusSeconds(3600));
        user.setEmailVerifiedAt(null);

        Community community = new Community(UUID.randomUUID(), "Comunidad", "Europe/Madrid", "es", "EUR",
                CommunityStatus.ACTIVE, now, now);
        Membership membership = new Membership(UUID.randomUUID(), community.getId(), user.getId(),
                MembershipStatus.ACTIVE, now, now, now);

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(NEW_PASSWORD)).thenReturn("new-hash");
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(communityRepository.findAll()).thenReturn(List.of(community));
        when(membershipRepository.findByCommunityIdAndUserId(community.getId(), user.getId()))
                .thenReturn(Optional.of(membership));
        when(membershipRoleRepository.findRoleCodesByMembershipId(membership.getId()))
                .thenReturn(List.of("COMMUNITY_ADMIN"));

        initializer(true).run();

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void doesNotTouchExistingAdminWhenResetIsDisabled() {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), EMAIL, "old-hash", "Administrador", "es",
                UserStatus.ACTIVE, now, now);
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));

        initializer(false).run();

        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(AppUser.class));
    }

    private BootstrapAdminInitializer initializer(boolean resetPassword) {
        return new BootstrapAdminInitializer(userRepository, communityRepository, membershipRepository,
                membershipRoleRepository, passwordEncoder, EMAIL, NEW_PASSWORD, "Comunidad", resetPassword);
    }
}
