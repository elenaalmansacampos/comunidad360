package com.comunidad360.community;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

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
    void dashboardShowsRoleScopedWidgetsAndFinance() throws Exception {
        AppUser admin = createUser(uniqueEmail("dashboard-admin"));
        AppUser owner = createUser(uniqueEmail("dashboard-owner"));
        Community community = createCommunity("Dashboard Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());

        UUID fiscalYearId = createFiscalYear(adminToken, community.getId(), 2026);
        createLedger(adminToken, community.getId(), fiscalYearId, "INCOME", "Cuotas", "200.00");
        createLedger(adminToken, community.getId(), fiscalYearId, "EXPENSE", "Limpieza", "80.00");
        createIssuedAssessment(adminToken, community.getId(), fiscalYearId);

        createIncident(ownerToken, community.getId(), unitId, "Fuga en garaje");
        createMeeting(adminToken, community.getId(), "Junta ordinaria");

        mockMvc.perform(get(dashboardPath(community.getId()))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeMembers").value(2))
                .andExpect(jsonPath("$.openIncidents").value(1))
                .andExpect(jsonPath("$.finance.scope").value("COMMUNITY"))
                .andExpect(jsonPath("$.finance.income").value("200.00"))
                .andExpect(jsonPath("$.finance.expense").value("80.00"))
                .andExpect(jsonPath("$.finance.balance").value("120.00"))
                .andExpect(jsonPath("$.finance.pendingCharges").value("100.00"))
                .andExpect(jsonPath("$.recentIncidents.length()").value(1))
                .andExpect(jsonPath("$.upcomingMeetings.length()").value(1))
                .andExpect(jsonPath("$.monthlyFinance.length()").value(12))
                .andExpect(jsonPath("$.canViewReports").value(true));

        mockMvc.perform(get(dashboardPath(community.getId()))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finance.scope").value("OWN"))
                .andExpect(jsonPath("$.finance.pendingCharges").value("100.00"))
                .andExpect(jsonPath("$.finance.income").value("0.00"))
                .andExpect(jsonPath("$.canViewReports").value(false));

        MvcResult report = mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/reports/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();
        byte[] pdf = report.getResponse().getContentAsByteArray();
        assertThat(pdf.length).isGreaterThan(500);
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/reports/summary")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardWithoutFiscalYearDoesNotFail() throws Exception {
        AppUser admin = createUser(uniqueEmail("dashboard-empty-admin"));
        Community community = createCommunity("Dashboard Empty");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        mockMvc.perform(get(dashboardPath(community.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finance").isEmpty())
                .andExpect(jsonPath("$.openIncidents").value(0))
                .andExpect(jsonPath("$.monthlyFinance.length()").value(0));
    }

    @Test
    void crossCommunityDashboardIsRejected() throws Exception {
        AppUser adminA = createUser(uniqueEmail("dashboard-cross-a"));
        AppUser adminB = createUser(uniqueEmail("dashboard-cross-b"));
        Community communityA = createCommunity("Dashboard Cross A");
        Community communityB = createCommunity("Dashboard Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenB = accessToken(adminB.getEmail());

        mockMvc.perform(get(dashboardPath(communityA.getId()))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    private UUID createFiscalYear(String token, UUID communityId, int year) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/fiscal-years")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "year", year,
                                "startDate", LocalDate.of(year, 1, 1).toString(),
                                "endDate", LocalDate.of(year, 12, 31).toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private void createLedger(String token, UUID communityId, UUID fiscalYearId, String type,
                              String category, String amount) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/ledger-entries")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fiscalYearId", fiscalYearId.toString(),
                                "entryDate", LocalDate.now().toString(),
                                "type", type,
                                "category", category,
                                "description", category,
                                "amount", amount))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID entryId = UUID.fromString(json(result).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/ledger-entries/"
                        + entryId + "/post")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void createIssuedAssessment(String token, UUID communityId, UUID fiscalYearId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/assessments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fiscalYearId", fiscalYearId.toString(),
                                "concept", "Cuota ordinaria",
                                "type", "ORDINARY",
                                "periodStart", LocalDate.now().toString(),
                                "periodEnd", LocalDate.now().plusMonths(1).toString(),
                                "dueDate", LocalDate.now().plusMonths(1).toString(),
                                "totalAmount", "100.00",
                                "allocationMethod", "COEFFICIENT"))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID assessmentId = UUID.fromString(json(result).get("id").asText());
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/finance/assessments/"
                        + assessmentId + "/issue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void createIncident(String token, UUID communityId, UUID unitId, String title) throws Exception {
        mockMvc.perform(post("/api/v1/communities/" + communityId + "/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title,
                                "description", "Incidencia de prueba",
                                "category", "GENERAL",
                                "priority", "MEDIUM",
                                "unitId", unitId.toString()))))
                .andExpect(status().isCreated());
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

    private String dashboardPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/dashboard";
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
                "Dashboard Tester", "es", UserStatus.ACTIVE, now, now);
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
