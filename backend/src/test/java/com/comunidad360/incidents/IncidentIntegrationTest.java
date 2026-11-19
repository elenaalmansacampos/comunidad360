package com.comunidad360.incidents;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipRole;
import com.comunidad360.membership.MembershipRoleRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.property.Unit;
import com.comunidad360.property.UnitRepository;
import com.comunidad360.property.UnitType;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl2ZQAAAABJRU5ErkJggg=="
    );

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
    UnitRepository unitRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void visibilityAndStatusTransitionsWork() throws Exception {
        AppUser admin = createUser(uniqueEmail("incident-admin"));
        AppUser owner = createUser(uniqueEmail("incident-owner"));
        AppUser otherOwner = createUser(uniqueEmail("incident-other"));
        Community community = createCommunity("Incident Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, otherOwner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        UUID otherUnitId = createUnit(community, "1B");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String otherToken = accessToken(otherOwner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());

        UUID incidentId = createIncident(adminToken, community.getId(), unitId,
                "Fuga en garaje", "Hay una fuga de agua", "FONTANERIA", "HIGH");

        mockMvc.perform(get(incidentsPath(community.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentId.toString()));
        mockMvc.perform(get(incidentsPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(incidentId.toString()));
        mockMvc.perform(get(incidentsPath(community.getId()))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        createIncident(ownerToken, community.getId(), unitId,
                "Luz escalera", "La luz no funciona", "ELECTRICIDAD", "MEDIUM");
        mockMvc.perform(get(incidentsPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(patch(incidentsPath(community.getId()) + "/" + incidentId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isForbidden());

        updateStatus(adminToken, community.getId(), incidentId, "IN_PROGRESS")
                .andExpect(status().isOk());
        updateStatus(adminToken, community.getId(), incidentId, "RESOLVED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        updateStatus(adminToken, community.getId(), incidentId, "CLOSED")
                .andExpect(status().isOk());
        updateStatus(adminToken, community.getId(), incidentId, "IN_PROGRESS")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS_TRANSITION"));

        mockMvc.perform(get(incidentsPath(community.getId()) + "/" + incidentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incident.status").value("CLOSED"))
                .andExpect(jsonPath("$.history.length()").value(4));
    }

    @Test
    void commentsRespectInternalVisibility() throws Exception {
        AppUser admin = createUser(uniqueEmail("incident-comment-admin"));
        AppUser owner = createUser(uniqueEmail("incident-comment-owner"));
        Community community = createCommunity("Incident Comments Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());
        UUID incidentId = createIncident(adminToken, community.getId(), unitId,
                "Ruido", "Ruido en el patio", "CONVIVENCIA", "MEDIUM");

        mockMvc.perform(post(incidentsPath(community.getId()) + "/" + incidentId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("body", "Comentario público"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(false));

        mockMvc.perform(post(incidentsPath(community.getId()) + "/" + incidentId + "/comments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "body", "Nota interna",
                                "internal", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.internal").value(true));

        JsonNode ownerDetail = detail(ownerToken, community.getId(), incidentId);
        assertThat(ownerDetail.get("comments")).hasSize(1);
        assertThat(ownerDetail.get("comments").get(0).get("body").asText()).isEqualTo("Comentario público");

        JsonNode adminDetail = detail(adminToken, community.getId(), incidentId);
        assertThat(adminDetail.get("comments")).hasSize(2);
        assertThat(adminDetail.get("comments").get(1).get("internal").asBoolean()).isTrue();
    }

    @Test
    void ownerCanUploadPhotoAndOtherOwnerCannotDownloadIt() throws Exception {
        AppUser admin = createUser(uniqueEmail("incident-photo-admin"));
        AppUser owner = createUser(uniqueEmail("incident-photo-owner"));
        AppUser otherOwner = createUser(uniqueEmail("incident-photo-other"));
        Community community = createCommunity("Incident Photo Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, otherOwner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String otherToken = accessToken(otherOwner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());
        UUID incidentId = createIncident(ownerToken, community.getId(), unitId,
                "Humedad", "Mancha de humedad", "HUMEDAD", "HIGH");

        MockMultipartFile photo = new MockMultipartFile("file", "foto.png", "image/png", PNG);
        MvcResult uploadResult = mockMvc.perform(multipart(
                        incidentsPath(community.getId()) + "/" + incidentId + "/attachments")
                        .file(photo)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("foto.png"))
                .andExpect(jsonPath("$.mimeType").value("image/png"))
                .andReturn();
        UUID attachmentId = UUID.fromString(json(uploadResult).get("id").asText());

        mockMvc.perform(get(incidentsPath(community.getId()) + "/" + incidentId
                        + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG));

        mockMvc.perform(get(incidentsPath(community.getId()) + "/" + incidentId
                        + "/attachments/" + attachmentId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        MockMultipartFile invalid = new MockMultipartFile(
                "file", "nota.txt", "text/plain", "hola".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(incidentsPath(community.getId()) + "/" + incidentId + "/attachments")
                        .file(invalid)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ATTACHMENT_TYPE_UNSUPPORTED"));
    }

    @Test
    void crossCommunityIncidentAccessIsRejected() throws Exception {
        AppUser adminA = createUser(uniqueEmail("incident-cross-a"));
        AppUser adminB = createUser(uniqueEmail("incident-cross-b"));
        Community communityA = createCommunity("Incident Cross A");
        Community communityB = createCommunity("Incident Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        UUID unitA = createUnit(communityA, "1A");
        String tokenA = accessToken(adminA.getEmail());
        String tokenB = accessToken(adminB.getEmail());
        UUID incidentId = createIncident(tokenA, communityA.getId(), unitA,
                "Incidencia privada", "No debe verse", "GENERAL", "LOW");

        mockMvc.perform(get(incidentsPath(communityA.getId()) + "/" + incidentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    private ResultActions updateStatus(String token, UUID communityId, UUID incidentId, String status)
            throws Exception {
        return mockMvc.perform(patch(incidentsPath(communityId) + "/" + incidentId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("status", status))));
    }

    private JsonNode detail(String token, UUID communityId, UUID incidentId) throws Exception {
        return json(mockMvc.perform(get(incidentsPath(communityId) + "/" + incidentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private UUID createIncident(String token, UUID communityId, UUID unitId, String title,
                                String description, String category, String priority) throws Exception {
        MvcResult result = mockMvc.perform(post(incidentsPath(communityId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", description,
                                "category", category,
                                "priority", priority,
                                "unitId", unitId.toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private void createOwnership(String token, UUID communityId, UUID unitId, UUID ownerId) throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/units/" + unitId + "/ownerships")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "userId", ownerId.toString(),
                                "ownershipType", "OWNER",
                                "sharePercentage", "100.0000",
                                "validFrom", LocalDate.now().toString()))))
                .andExpect(status().isCreated());
    }

    private String incidentsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/incidents";
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
        AppUser user = new AppUser(
                UUID.randomUUID(),
                email,
                passwordEncoder.encode("Password123!"),
                "Incident Tester",
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

    private UUID createUnit(Community community, String code) {
        Instant now = Instant.now();
        Unit unit = new Unit(
                UUID.randomUUID(),
                community.getId(),
                code,
                UnitType.VIVIENDA,
                new BigDecimal("100.000000"),
                now,
                now
        );
        return unitRepository.save(unit).getId();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
