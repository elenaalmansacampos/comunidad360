package com.comunidad360.authorization;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIntegrationTest {

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
    void ownerDelegatesScopesAndViewerActsOnBehalf() throws Exception {
        AppUser admin = createUser("auth-admin");
        AppUser owner = createUser("auth-owner");
        AppUser viewer = createUser("auth-viewer");
        Community community = createCommunity("Authorization Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        Membership ownerMembership = createMembership(community, owner, "OWNER");
        Membership viewerMembership = createMembership(community, viewer, "VIEWER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String viewerToken = accessToken(viewer.getEmail());

        for (String scope : new String[]{"VOTE", "WORK_PERMIT", "INCIDENT", "BOARD", "LOST_FOUND"}) {
            mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/authorizations")
                            .header("Authorization", "Bearer " + ownerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "granteeMembershipId", viewerMembership.getId(),
                                    "scope", scope))))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/authorizations?direction=RECEIVED")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));

        String onBehalf = "?onBehalfOfMembershipId=" + ownerMembership.getId();
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/work-permits" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Permiso delegado"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/incidents" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Incidencia delegada", "description", "Detalle",
                                "category", "General", "priority", "MEDIUM"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/board-notes" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Nota delegada", "body", "Contenido", "color", "YELLOW"))))
                .andExpect(status().isCreated());
        MvcResult lost = mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/lost-items" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Objeto delegado", "type", "LOST"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID lostId = UUID.fromString(json(lost).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/lost-items/" + lostId + "/notes" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Nota delegada"))))
                .andExpect(status().isCreated());

        MvcResult voting = mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/votings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Votacion delegada",
                                "closesAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                                "options", java.util.List.of("Si", "No")))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID votingId = UUID.fromString(json(voting).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/votings/" + votingId + "/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        MvcResult detail = mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/votings/" + votingId)
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID optionId = UUID.fromString(json(detail).get("options").get(0).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/votings/" + votingId + "/votes" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", optionId))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/votings/" + votingId + "/results")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participatingEligible").value(1));

        mockMvc.perform(multipart("/api/v1/communities/" + community.getId() + "/lost-items/" + lostId + "/attachments" + onBehalf)
                        .file(new MockMultipartFile("file", "aviso.pdf", "application/pdf",
                                "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isCreated());

        MvcResult given = mockMvc.perform(get("/api/v1/communities/" + community.getId()
                        + "/authorizations?direction=GRANTED")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID workPermitAuthorization = null;
        for (JsonNode node : json(given)) {
            if ("WORK_PERMIT".equals(node.get("scope").asText())) {
                workPermitAuthorization = UUID.fromString(node.get("id").asText());
            }
        }
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/v1/communities/" + community.getId() + "/authorizations/" + workPermitAuthorization)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/work-permits" + onBehalf)
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Revocado"))))
                .andExpect(status().isForbidden());

        AppUser vecino = createUser("auth-vecino");
        createMembership(community, vecino, "VECINO");
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/authorizations")
                        .header("Authorization", "Bearer " + accessToken(vecino.getEmail()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "granteeMembershipId", viewerMembership.getId(),
                                "scope", "VOTE"))))
                .andExpect(status().isUnprocessableEntity());
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

    private AppUser createUser(String prefix) {
        Instant now = Instant.now();
        String email = prefix + "-" + UUID.randomUUID() + "@example.com";
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                prefix, "es", UserStatus.ACTIVE, now, now);
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
}
