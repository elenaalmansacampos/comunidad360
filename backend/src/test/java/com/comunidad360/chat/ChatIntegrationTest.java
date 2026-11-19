package com.comunidad360.chat;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired CommunityRepository communityRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MembershipRoleRepository membershipRoleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void generalChannelAndMessagesAreShared() throws Exception {
        AppUser admin = createUser(uniqueEmail("chat-admin"));
        AppUser owner = createUser(uniqueEmail("chat-owner"));
        Community community = createCommunity("Chat Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());

        MvcResult channels = mockMvc.perform(get(chatPath(community.getId()) + "/channels")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("GENERAL"))
                .andReturn();
        UUID channelId = UUID.fromString(json(channels).get(0).get("id").asText());

        mockMvc.perform(post(chatPath(community.getId()) + "/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hola a todos"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("Hola a todos"));

        mockMvc.perform(get(chatPath(community.getId()) + "/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Hola a todos"));
    }

    @Test
    void privateChannelIsOnlyVisibleToMembers() throws Exception {
        AppUser admin = createUser(uniqueEmail("chat-private-admin"));
        AppUser owner = createUser(uniqueEmail("chat-private-owner"));
        AppUser other = createUser(uniqueEmail("chat-private-other"));
        Community community = createCommunity("Chat Private");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        Membership ownerMembership = createMembership(community, owner, "OWNER");
        createMembership(community, other, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String otherToken = accessToken(other.getEmail());

        MvcResult created = mockMvc.perform(post(chatPath(community.getId()) + "/channels")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "type", "PRIVATE",
                                "name", "Comisión solar",
                                "description", "Canal privado",
                                "memberMembershipIds", List.of(ownerMembership.getId().toString())))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("PRIVATE"))
                .andReturn();
        UUID channelId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(get(chatPath(community.getId()) + "/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get(chatPath(community.getId()) + "/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatIsIsolatedByCommunity() throws Exception {
        AppUser adminA = createUser(uniqueEmail("chat-cross-a"));
        AppUser adminB = createUser(uniqueEmail("chat-cross-b"));
        Community communityA = createCommunity("Chat Cross A");
        Community communityB = createCommunity("Chat Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenB = accessToken(adminB.getEmail());

        mockMvc.perform(get(chatPath(communityA.getId()) + "/channels")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    private String chatPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/chat";
    }
    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Password123!"))))
                .andExpect(status().isOk()).andReturn();
        return json(result).get("accessToken").asText();
    }
    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                "Chat Tester", "es", UserStatus.ACTIVE, now, now);
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
    private String uniqueEmail(String prefix) { return prefix + "-" + UUID.randomUUID() + "@example.com"; }
}
