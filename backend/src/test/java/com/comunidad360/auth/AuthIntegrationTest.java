package com.comunidad360.auth;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.security.CookieService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    AppUserRepository userRepository;

    @Autowired
    CommunityRepository communityRepository;

    @Autowired
    MembershipRepository membershipRepository;

    @Autowired
    MembershipRoleRepository membershipRoleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthService authService;

    @Test
    void loginReturnsAccessTokenAndRefreshCookie() throws Exception {
        AppUser user = createUser(uniqueEmail("login"));

        MvcResult result = login(user.getEmail())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(CookieService.REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void loginWithWrongPasswordReturnsUnauthorized() throws Exception {
        AppUser user = createUser(uniqueEmail("wrong-password"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "password", "WrongPassword123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void logoutRevokesRefreshCookieWithoutAccessToken() throws Exception {
        AppUser user = createUser(uniqueEmail("logout"));
        MvcResult loginResult = login(user.getEmail()).andReturn();
        Cookie cookie = loginResult.getResponse().getCookie(CookieService.REFRESH_COOKIE);
        assertThat(cookie).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() throws Exception {
        AppUser user = createUser(uniqueEmail("refresh"));
        MvcResult loginResult = login(user.getEmail()).andReturn();
        Cookie original = loginResult.getResponse().getCookie(CookieService.REFRESH_COOKIE);
        assertThat(original).isNotNull();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie rotated = refreshResult.getResponse().getCookie(CookieService.REFRESH_COOKIE);
        assertThat(rotated).isNotNull();
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(original))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_REUSED"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(rotated))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_REUSED"));
    }

    @Test
    void userCannotAccessAnotherCommunity() throws Exception {
        AppUser userA = createUser(uniqueEmail("tenant-a"));
        AppUser userB = createUser(uniqueEmail("tenant-b"));
        Community communityA = createCommunity("Community A");
        Community communityB = createCommunity("Community B");
        createMembership(communityA, userA, "COMMUNITY_ADMIN");
        createMembership(communityB, userB, "COMMUNITY_ADMIN");

        String token = accessToken(userA.getEmail());

        mockMvc.perform(get("/api/v1/communities/" + communityA.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Community A"));

        mockMvc.perform(get("/api/v1/communities/" + communityB.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_FORBIDDEN"));
    }

    @Test
    void ownerCannotListMembersWithoutPermission() throws Exception {
        AppUser owner = createUser(uniqueEmail("owner"));
        Community community = createCommunity("Owner Community");
        createMembership(community, owner, "OWNER");

        String token = accessToken(owner.getEmail());

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/members")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void invitationAcceptanceCreatesMembershipAndSession() throws Exception {
        AppUser admin = createUser(uniqueEmail("admin"));
        Community community = createCommunity("Invitation Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String inviteeEmail = uniqueEmail("invitee");

        AuthService.CreatedInvitation invitation = authService.createInvitation(
                community.getId(), admin.getId(), inviteeEmail, "OWNER");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "token", invitation.rawToken(),
                                "fullName", "Invited Owner",
                                "password", "Password123!"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(inviteeEmail))
                .andExpect(jsonPath("$.memberships[0].roles[0]").value("OWNER"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", email,
                        "password", "Password123!"))));
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = login(email).andExpect(status().isOk()).andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode("Password123!"),
                "Test User",
                "es",
                UserStatus.ACTIVE,
                now,
                now
        );
        user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }

    private Community createCommunity(String name) {
        Instant now = Instant.now();
        return communityRepository.save(new Community(
                UUID.randomUUID(),
                name,
                "Europe/Madrid",
                "es",
                "EUR",
                CommunityStatus.ACTIVE,
                now,
                now
        ));
    }

    private Membership createMembership(Community community, AppUser user, String roleCode) {
        Instant now = Instant.now();
        Membership membership = membershipRepository.save(new Membership(
                UUID.randomUUID(),
                community.getId(),
                user.getId(),
                MembershipStatus.ACTIVE,
                now,
                now,
                now
        ));
        membershipRoleRepository.save(new MembershipRole(
                UUID.randomUUID(),
                membership.getId(),
                roleCode,
                now,
                user.getId()
        ));
        return membership;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
