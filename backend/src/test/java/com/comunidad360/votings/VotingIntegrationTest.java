package com.comunidad360.votings;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.*;
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
import java.time.temporal.ChronoUnit;
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
class VotingIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired CommunityRepository communityRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MembershipRoleRepository membershipRoleRepository;
    @Autowired UnitRepository unitRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void consultiveVotingHandlesEligibilityVoteAndResults() throws Exception {
        AppUser admin = createUser(uniqueEmail("vote-admin"));
        AppUser owner = createUser(uniqueEmail("vote-owner"));
        Community community = createCommunity("Voting Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        createOwnership(adminToken, community.getId(), unitId, owner.getId());

        MvcResult created = mockMvc.perform(post(votingsPath(community.getId()))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "¿Instalar placas solares?",
                                "description", "Consulta informativa",
                                "opensAt", Instant.now().toString(),
                                "closesAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                                "options", List.of("Sí", "No")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.legalNotice").isNotEmpty())
                .andReturn();
        UUID votingId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(post(votingsPath(community.getId()) + "/" + votingId + "/open")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.totalEligible").value(2));

        JsonNode detail = json(mockMvc.perform(get(votingsPath(community.getId()) + "/" + votingId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voting.hasVoted").value(false))
                .andExpect(jsonPath("$.options.length()").value(2))
                .andReturn());
        UUID optionId = UUID.fromString(detail.get("options").get(0).get("id").asText());

        mockMvc.perform(post(votingsPath(community.getId()) + "/" + votingId + "/votes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", optionId.toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voting.hasVoted").value(true));

        mockMvc.perform(post(votingsPath(community.getId()) + "/" + votingId + "/votes")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("optionId", optionId.toString()))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTE_ALREADY_CAST"));

        mockMvc.perform(post(votingsPath(community.getId()) + "/" + votingId + "/close")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get(votingsPath(community.getId()) + "/" + votingId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.totalEligible").value(2))
                .andExpect(jsonPath("$.results.participatingEligible").value(1))
                .andExpect(jsonPath("$.results.options[0].votes").value(1));
    }

    @Test
    void votingIsIsolatedByCommunity() throws Exception {
        AppUser adminA = createUser(uniqueEmail("vote-cross-a"));
        AppUser adminB = createUser(uniqueEmail("vote-cross-b"));
        Community communityA = createCommunity("Voting Cross A");
        Community communityB = createCommunity("Voting Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenA = accessToken(adminA.getEmail());
        String tokenB = accessToken(adminB.getEmail());
        MvcResult created = mockMvc.perform(post(votingsPath(communityA.getId()))
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Votación privada",
                                "closesAt", Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                                "options", List.of("Sí", "No")))))
                .andExpect(status().isCreated()).andReturn();
        UUID votingId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(get(votingsPath(communityA.getId()) + "/" + votingId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
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
    private UUID createUnit(Community community, String code) {
        Instant now = Instant.now();
        Unit unit = new Unit(UUID.randomUUID(), community.getId(), code, UnitType.VIVIENDA,
                new BigDecimal("100.000000"), now, now);
        return unitRepository.save(unit).getId();
    }
    private String votingsPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/votings";
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
                "Voting Tester", "es", UserStatus.ACTIVE, now, now);
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
