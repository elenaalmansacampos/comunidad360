package com.comunidad360.property;

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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
class StructureIntegrationTest {

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
    void adminCanManageStructureAndSeeCoefficientSummary() throws Exception {
        AppUser admin = createUser(uniqueEmail("structure-admin"));
        Community community = createCommunity("Structure Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        UUID buildingId = createBuilding(token, community.getId(), "Edificio A");
        createUnit(token, community.getId(), "1A", "60.000000", buildingId)
                .andExpect(status().isCreated());
        createUnit(token, community.getId(), "1B", "40.000000", null)
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/common-elements")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Ascensor",
                                "type", "ELEVADOR",
                                "buildingId", buildingId.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ascensor"));

        MvcResult summaryResult = mockMvc.perform(get(
                        "/api/v1/communities/" + community.getId() + "/structure/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.totalUnits").value(2))
                .andExpect(jsonPath("$.activeUnits").value(2))
                .andExpect(jsonPath("$.buildings").value(1))
                .andExpect(jsonPath("$.commonElements").value(1))
                .andReturn();

        JsonNode summary = json(summaryResult);
        assertThat(new BigDecimal(summary.get("coefficientSum").asText()))
                .isEqualByComparingTo(new BigDecimal("100.000000"));
        assertThat(new BigDecimal(summary.get("tolerance").asText()))
                .isEqualByComparingTo(new BigDecimal("0.0001"));
    }

    @Test
    void ownerCanViewButNotManageStructure() throws Exception {
        AppUser owner = createUser(uniqueEmail("structure-owner"));
        Community community = createCommunity("Owner Structure Community");
        createMembership(community, owner, "OWNER");
        String token = accessToken(owner.getEmail());

        mockMvc.perform(get("/api/v1/communities/" + community.getId() + "/buildings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/communities/" + community.getId() + "/buildings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Edificio prohibido"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void userCannotAccessAnotherCommunityStructure() throws Exception {
        AppUser userA = createUser(uniqueEmail("structure-a"));
        AppUser userB = createUser(uniqueEmail("structure-b"));
        Community communityA = createCommunity("Structure A");
        Community communityB = createCommunity("Structure B");
        createMembership(communityA, userA, "COMMUNITY_ADMIN");
        createMembership(communityB, userB, "COMMUNITY_ADMIN");
        String token = accessToken(userA.getEmail());

        mockMvc.perform(get("/api/v1/communities/" + communityB.getId() + "/buildings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_FORBIDDEN"));

        mockMvc.perform(get("/api/v1/communities/" + communityB.getId() + "/structure/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicateUnitCodeIsRejected() throws Exception {
        AppUser admin = createUser(uniqueEmail("structure-duplicate"));
        Community community = createCommunity("Duplicate Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        createUnit(token, community.getId(), "1A", "100.000000", null)
                .andExpect(status().isCreated());
        createUnit(token, community.getId(), "1A", "0.000000", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("UNIT_CODE_EXISTS"));
    }

    @Test
    void unitCannotReferenceBuildingFromAnotherCommunity() throws Exception {
        AppUser userA = createUser(uniqueEmail("structure-invalid-a"));
        AppUser userB = createUser(uniqueEmail("structure-invalid-b"));
        Community communityA = createCommunity("Invalid Building A");
        Community communityB = createCommunity("Invalid Building B");
        createMembership(communityA, userA, "COMMUNITY_ADMIN");
        createMembership(communityB, userB, "COMMUNITY_ADMIN");
        String tokenB = accessToken(userB.getEmail());
        UUID buildingB = createBuilding(tokenB, communityB.getId(), "Edificio B");

        String tokenA = accessToken(userA.getEmail());
        createUnit(tokenA, communityA.getId(), "1A", "100.000000", buildingB)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_BUILDING"));
    }

    @Test
    void summaryDetectsInvalidCoefficientSumAndArchiveRules() throws Exception {
        AppUser admin = createUser(uniqueEmail("structure-sum"));
        Community community = createCommunity("Coefficient Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String token = accessToken(admin.getEmail());

        UUID buildingId = createBuilding(token, community.getId(), "Edificio C");
        MvcResult firstUnit = createUnit(token, community.getId(), "1A", "60.000000", buildingId)
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstUnitId = UUID.fromString(json(firstUnit).get("id").asText());
        MvcResult secondUnit = createUnit(token, community.getId(), "1B", "40.000000", buildingId)
                .andExpect(status().isCreated())
                .andReturn();
        UUID secondUnitId = UUID.fromString(json(secondUnit).get("id").asText());

        JsonNode validSummary = summary(token, community.getId());
        assertThat(validSummary.get("valid").asBoolean()).isTrue();

        mockMvc.perform(patch("/api/v1/communities/" + community.getId() + "/units/" + secondUnitId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "coefficient", "30.000000",
                                "version", json(secondUnit).get("version").asLong()))))
                .andExpect(status().isOk());

        JsonNode invalidSummary = summary(token, community.getId());
        assertThat(invalidSummary.get("valid").asBoolean()).isFalse();
        assertThat(new BigDecimal(invalidSummary.get("coefficientSum").asText()))
                .isEqualByComparingTo(new BigDecimal("90.000000"));

        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/units/" + secondUnitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/buildings/" + buildingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BUILDING_HAS_UNITS"));

        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/units/" + firstUnitId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/communities/" + community.getId() + "/buildings/" + buildingId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    private JsonNode summary(String token, UUID communityId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/communities/" + communityId + "/structure/summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    private UUID createBuilding(String token, UUID communityId, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/communities/" + communityId + "/buildings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(result).get("id").asText());
    }

    private org.springframework.test.web.servlet.ResultActions createUnit(
            String token, UUID communityId, String code, String coefficient, UUID buildingId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("type", "VIVIENDA");
        body.put("coefficient", coefficient);
        if (buildingId != null) {
            body.put("buildingId", buildingId.toString());
        }
        return mockMvc.perform(post("/api/v1/communities/" + communityId + "/units")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
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
                "Structure Tester",
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
