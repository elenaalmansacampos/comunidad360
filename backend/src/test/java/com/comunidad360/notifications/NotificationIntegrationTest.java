package com.comunidad360.notifications;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationIntegrationTest {

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

    @Test
    void preferencesControlInAppNotificationsAndReadState() throws Exception {
        AppUser admin = createUser(uniqueEmail("notification-admin"));
        AppUser owner = createUser(uniqueEmail("notification-owner"));
        Community community = createCommunity("Notification Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());

        mockMvc.perform(get("/api/v1/me/notification-preferences")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));

        setPreferences(ownerToken, false);
        createMeeting(adminToken, community.getId(), "Sin aviso");
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        setPreferences(ownerToken, true);
        createMeeting(adminToken, community.getId(), "Con aviso");
        MvcResult listResult = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SENT"))
                .andReturn();
        UUID notificationId = UUID.fromString(json(listResult).get(0).get("id").asText());

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READ"));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void notificationsArePrivateAndCanBeMarkedAllRead() throws Exception {
        AppUser admin = createUser(uniqueEmail("notification-private-admin"));
        AppUser ownerA = createUser(uniqueEmail("notification-private-a"));
        AppUser ownerB = createUser(uniqueEmail("notification-private-b"));
        Community community = createCommunity("Notification Private");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, ownerA, "OWNER");
        createMembership(community, ownerB, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String tokenA = accessToken(ownerA.getEmail());
        String tokenB = accessToken(ownerB.getEmail());

        createMeeting(adminToken, community.getId(), "Reunión privada");
        MvcResult listA = mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn();
        UUID notificationId = UUID.fromString(json(listA).get(0).get("id").asText());

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(1));

        mockMvc.perform(get("/api/v1/notifications?unreadOnly=true")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private void setPreferences(String token, boolean enabled) throws Exception {
        List<Map<String, Object>> preferences = List.of(
                Map.of("category", "MEETINGS", "channel", "IN_APP", "enabled", enabled),
                Map.of("category", "MEETINGS", "channel", "EMAIL", "enabled", enabled)
        );
        mockMvc.perform(put("/api/v1/me/notification-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("preferences", preferences))))
                .andExpect(status().isOk());
    }

    private void createMeeting(String token, UUID communityId, String title) throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/meetings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "type", "ORDINARY",
                                "scheduledStart", Instant.now().plusSeconds(3600).toString(),
                                "status", "CONVENED"))))
                .andExpect(status().isCreated());
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return json(result).get("accessToken").asText();
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                "Notification Tester", "es", UserStatus.ACTIVE, now, now);
        user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }

    private Community createCommunity(String name) {
        Instant now = Instant.now();
        return communityRepository.save(new Community(UUID.randomUUID(), name, "Europe/Madrid",
                "es", "EUR", CommunityStatus.ACTIVE, now, now));
    }

    private Membership createMembership(Community community, AppUser user, String roleCode) {
        Instant now = Instant.now();
        Membership membership = membershipRepository.save(new Membership(UUID.randomUUID(),
                community.getId(), user.getId(), MembershipStatus.ACTIVE, now, now, now));
        membershipRoleRepository.save(new MembershipRole(UUID.randomUUID(), membership.getId(),
                roleCode, now, user.getId()));
        return membership;
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
