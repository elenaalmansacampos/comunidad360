package com.comunidad360.lostfound;

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
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommunityExtrasIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired CommunityRepository communityRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MembershipRoleRepository membershipRoleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void lostItemsLifecycleAndTenantIsolation() throws Exception {
        AppUser admin = createUser(uniqueEmail("lost-admin"));
        Community community = createCommunity("Lost Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        MvcResult created = mockMvc.perform(post(path(community.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Llavero con llaves",
                                "description", "Llavero con mando de garaje",
                                "type", "LOST",
                                "location", "Portal"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn();
        JsonNode createdJson = json(created);
        UUID itemId = UUID.fromString(createdJson.get("id").asText());
        long version = createdJson.get("version").asLong();

        mockMvc.perform(get(path(community.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LOST"));

        mockMvc.perform(patch(path(community.getId(), "lost-items") + "/" + itemId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "RESOLVED",
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        AppUser other = createUser(uniqueEmail("lost-other"));
        Community otherCommunity = createCommunity("Lost Other");
        createMembership(otherCommunity, other, "COMMUNITY_ADMIN");
        String otherToken = accessToken(other.getEmail());

        mockMvc.perform(get(path(otherCommunity.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(delete(path(community.getId(), "lost-items") + "/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void boardNotesLifecycle() throws Exception {
        AppUser admin = createUser(uniqueEmail("board-admin"));
        Community community = createCommunity("Board Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        MvcResult created = mockMvc.perform(post(path(community.getId(), "board-notes"))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Reunion de vecinos",
                                "body", "Jueves a las 19:00 en el salon de actos",
                                "color", "YELLOW"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pinned").value(false))
                .andReturn();
        JsonNode createdJson = json(created);
        UUID noteId = UUID.fromString(createdJson.get("id").asText());
        long version = createdJson.get("version").asLong();

        mockMvc.perform(get(path(community.getId(), "board-notes"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch(path(community.getId(), "board-notes") + "/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "pinned", true,
                                "version", version))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinned").value(true));

        mockMvc.perform(delete(path(community.getId(), "board-notes") + "/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        AppUser admin = createUser(uniqueEmail("viewer-admin"));
        AppUser viewer = createUser(uniqueEmail("viewer-user"));
        Community community = createCommunity("Viewer Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, viewer, "VIEWER");
        String adminToken = accessToken(admin.getEmail());
        String viewerToken = accessToken(viewer.getEmail());

        mockMvc.perform(post(path(community.getId(), "board-notes"))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Aviso", "body", "Contenido", "color", "YELLOW"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post(path(community.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Objeto", "type", "LOST"))))
                .andExpect(status().isCreated());

        mockMvc.perform(get(path(community.getId(), "board-notes"))
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get(path(community.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post(path(community.getId(), "board-notes"))
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "No", "body", "No", "color", "BLUE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(path(community.getId(), "lost-items"))
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "No", "type", "FOUND"))))
                .andExpect(status().isForbidden());

        MvcResult channels = mockMvc.perform(get(path(community.getId(), "chat/channels"))
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID channelId = UUID.fromString(json(channels).get(0).get("id").asText());
        mockMvc.perform(post(path(community.getId(), "chat/channels") + "/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hola"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(path(community.getId(), "invitations"))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", uniqueEmail("viewer-invite"), "role", "VIEWER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"))
                .andExpect(jsonPath("$.devToken").isNotEmpty());
    }

    private String path(UUID communityId, String resource) {
        return "/api/v1/communities/" + communityId + "/" + resource;
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
                .andExpect(status().isOk()).andReturn();
        return json(result).get("accessToken").asText();
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                "Extras Tester", "es", UserStatus.ACTIVE, now, now);
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
