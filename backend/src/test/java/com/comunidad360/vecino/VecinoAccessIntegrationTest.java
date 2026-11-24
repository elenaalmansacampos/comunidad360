package com.comunidad360.vecino;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VecinoAccessIntegrationTest {

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
    void adminGeneratesVecinoAccessAndVecinoCanUseLimitedWriteSections() throws Exception {
        AppUser admin = createUser(uniqueEmail("vecino-admin"));
        Community community = createCommunity("Vecino Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String adminToken = accessToken(admin.getEmail());

        MvcResult adminItem = mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/lost-items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Cartera", "type", "LOST"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID adminItemId = UUID.fromString(json(adminItem).get("id").asText());

        MvcResult generated = mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/vecino-access")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("VECINO"))
                .andExpect(jsonPath("$.email").isNotEmpty())
                .andExpect(jsonPath("$.password").isNotEmpty())
                .andReturn();
        String vecinoEmail = json(generated).get("email").asText();
        String vecinoPassword = json(generated).get("password").asText();

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", vecinoEmail,
                                "password", vecinoPassword))))
                .andExpect(status().isOk())
                .andReturn();
        String vecinoToken = json(login).get("accessToken").asText();

        mockMvc.perform(get("/api/v1/communities")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roles[0]").value("VECINO"));

        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/board-notes")
                        .header("Authorization", "Bearer " + vecinoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Aviso vecino", "body", "Contenido", "color", "YELLOW"))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/lost-items")
                        .header("Authorization", "Bearer " + vecinoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Objeto", "type", "LOST"))))
                .andExpect(status().isCreated());

        MvcResult channels = mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/chat/channels")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID channelId = UUID.fromString(json(channels).get(0).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + community.getId()
                        + "/chat/channels/" + channelId + "/messages")
                        .header("Authorization", "Bearer " + vecinoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Hola"))))
                .andExpect(status().isCreated());

        MvcResult note = mockMvc.perform(post("/api/v1/communities/" + community.getId()
                        + "/lost-items/" + adminItemId + "/notes")
                        .header("Authorization", "Bearer " + vecinoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Lo vi en el portal"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID noteId = UUID.fromString(json(note).get("id").asText());

        MockMultipartFile attachmentFile = new MockMultipartFile("file", "aviso.pdf",
                "application/pdf", "%PDF-1.4\n1 0 obj\n<<>>\nendobj\n".getBytes(StandardCharsets.UTF_8));
        MvcResult attachment = mockMvc.perform(multipart("/api/v1/communities/" + community.getId()
                        + "/lost-items/" + adminItemId + "/attachments")
                        .file(attachmentFile)
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("aviso.pdf"))
                .andReturn();
        UUID attachmentId = UUID.fromString(json(attachment).get("id").asText());

        MvcResult lostItems = mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/lost-items")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode adminItemNode = StreamSupport.stream(json(lostItems).spliterator(), false)
                .filter(node -> node.get("id").asText().equals(adminItemId.toString()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, adminItemNode.get("notes").size());
        assertEquals("Lo vi en el portal", adminItemNode.get("notes").get(0).get("body").asText());
        assertEquals(1, adminItemNode.get("attachments").size());

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/lost-items/" + adminItemId
                        + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"));

        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/lost-items/" + adminItemId
                        + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/lost-items/" + adminItemId
                        + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/work-permits")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/work-permits")
                        .header("Authorization", "Bearer " + vecinoToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "No"))))
                .andExpect(status().isForbidden());

        MvcResult regenerated = mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/vecino-access")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String newPassword = json(regenerated).get("password").asText();
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", vecinoEmail,
                                "password", vecinoPassword))))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", vecinoEmail,
                                "password", newPassword))))
                .andExpect(status().isOk());
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
                "Vecino Tester", "es", UserStatus.ACTIVE, now, now);
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
