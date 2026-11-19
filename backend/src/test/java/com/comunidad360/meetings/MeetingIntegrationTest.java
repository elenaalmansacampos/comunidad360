package com.comunidad360.meetings;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingIntegrationTest {

    private static final byte[] PDF = "%PDF-1.4\nActa\n%%EOF".getBytes(StandardCharsets.UTF_8);

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
    UnitRepository unitRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void meetingCreationAgendaAndPermissionsWork() throws Exception {
        AppUser admin = createUser(uniqueEmail("meeting-admin"));
        AppUser owner = createUser(uniqueEmail("meeting-owner"));
        Community community = createCommunity("Meeting Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());

        UUID meetingId = createMeeting(adminToken, community.getId(), "Junta ordinaria", "DRAFT");
        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/agenda")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Aprobación de cuentas",
                                "description", "Ejercicio 2026"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(1));

        mockMvc.perform(post(meetingsPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "No permitido",
                                "type", "ORDINARY",
                                "scheduledStart", Instant.now().plusSeconds(3600).toString()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(meetingsPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(meetingId.toString()));

        mockMvc.perform(patch(meetingsPath(community.getId()) + "/" + meetingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "CONVENED",
                                "version", 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONVENED"));

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("MEETING_CONVENED"));

        JsonNode detail = detail(ownerToken, community.getId(), meetingId);
        assertThat(detail.get("agenda")).hasSize(1);
        assertThat(detail.get("meeting").get("status").asText()).isEqualTo("CONVENED");
    }

    @Test
    void attendanceUsesCoefficientAndPreventsSelfRepresentation() throws Exception {
        AppUser admin = createUser(uniqueEmail("meeting-attendance-admin"));
        AppUser owner = createUser(uniqueEmail("meeting-attendance-owner"));
        AppUser other = createUser(uniqueEmail("meeting-attendance-other"));
        Community community = createCommunity("Meeting Attendance");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        Membership ownerMembership = createMembership(community, owner, "OWNER");
        Membership otherMembership = createMembership(community, other, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());

        UUID meetingId = createMeeting(adminToken, community.getId(), "Junta asistencia", "CONVENED");

        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "membershipId", ownerMembership.getId().toString(),
                                "attendanceType", "IN_PERSON"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coefficientSnapshot").value(100.000000));

        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "membershipId", ownerMembership.getId().toString(),
                                "attendanceType", "REPRESENTED",
                                "representedByMembershipId", otherMembership.getId().toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attendanceType").value("REPRESENTED"));

        JsonNode detail = detail(ownerToken, community.getId(), meetingId);
        assertThat(detail.get("attendance")).hasSize(1);

        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/attendance")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "membershipId", ownerMembership.getId().toString(),
                                "attendanceType", "REPRESENTED",
                                "representedByMembershipId", ownerMembership.getId().toString()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void agreementsAndMinutesAreLinkedToTheMeeting() throws Exception {
        AppUser admin = createUser(uniqueEmail("meeting-agreement-admin"));
        AppUser owner = createUser(uniqueEmail("meeting-agreement-owner"));
        Community community = createCommunity("Meeting Agreements");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());

        UUID meetingId = createMeeting(adminToken, community.getId(), "Junta acuerdos", "CONVENED");
        MvcResult agendaResult = mockMvc.perform(
                        post(meetingsPath(community.getId()) + "/" + meetingId + "/agenda")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("title", "Punto 1"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID agendaId = UUID.fromString(json(agendaResult).get("id").asText());

        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/agreements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "agendaItemId", agendaId.toString(),
                                "title", "Aprobar presupuesto",
                                "votesFor", 4,
                                "votesAgainst", 1,
                                "abstentions", 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adopted").value(true));

        UUID documentId = uploadDocument(adminToken, community.getId());
        mockMvc.perform(post(meetingsPath(community.getId()) + "/" + meetingId + "/minutes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("documentId", documentId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minutesDocumentId").value(documentId.toString()));

        JsonNode detail = detail(ownerToken, community.getId(), meetingId);
        assertThat(detail.get("agreements")).hasSize(1);
        assertThat(detail.get("meeting").get("status").asText()).isEqualTo("HELD");
    }

    @Test
    void crossCommunityMeetingAccessIsRejected() throws Exception {
        AppUser adminA = createUser(uniqueEmail("meeting-cross-a"));
        AppUser adminB = createUser(uniqueEmail("meeting-cross-b"));
        Community communityA = createCommunity("Meeting Cross A");
        Community communityB = createCommunity("Meeting Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenA = accessToken(adminA.getEmail());
        String tokenB = accessToken(adminB.getEmail());
        UUID meetingId = createMeeting(tokenA, communityA.getId(), "Reunión privada", "DRAFT");

        mockMvc.perform(get(meetingsPath(communityA.getId()) + "/" + meetingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    private UUID createMeeting(String token, UUID communityId, String title, String status) throws Exception {
        MvcResult result = mockMvc.perform(post(meetingsPath(communityId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "type", "ORDINARY",
                                "scheduledStart", Instant.now().plusSeconds(3600).toString(),
                                "location", "Salón de actos",
                                "status", status))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private JsonNode detail(String token, UUID communityId, UUID meetingId) throws Exception {
        return json(mockMvc.perform(get(meetingsPath(communityId) + "/" + meetingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
    }

    private UUID uploadDocument(String token, UUID communityId) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/communities/" + communityId + "/documents")
                        .file(new MockMultipartFile("file", "acta.pdf", "application/pdf", PDF))
                        .param("title", "Acta de la reunión")
                        .param("category", "ACTA")
                        .param("visibility", "COMMUNITY")
                        .header("Authorization", "Bearer " + token))
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

    private String meetingsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/meetings";
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
                "Meeting Tester", "es", UserStatus.ACTIVE, now, now);
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

    private UUID createUnit(Community community, String code) {
        Instant now = Instant.now();
        Unit unit = new Unit(UUID.randomUUID(), community.getId(), code, UnitType.VIVIENDA,
                new BigDecimal("100.000000"), now, now);
        return unitRepository.save(unit).getId();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
