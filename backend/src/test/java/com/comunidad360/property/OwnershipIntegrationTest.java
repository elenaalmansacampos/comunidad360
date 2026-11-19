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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnershipIntegrationTest {

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
    void adminCanManageOwnershipAndTenancy() throws Exception {
        AppUser admin = createUser(uniqueEmail("ownership-admin"));
        AppUser owner = createUser(uniqueEmail("ownership-owner"));
        AppUser tenant = createUser(uniqueEmail("ownership-tenant"));
        Community community = createCommunity("Ownership Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, tenant, "TENANT");
        UUID unitId = createUnit(community, "1A");

        String adminToken = accessToken(admin.getEmail());
        MvcResult ownershipResult = createOwnership(adminToken, community.getId(), unitId,
                owner.getId(), "60.0000", LocalDate.now(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("Ownership Tester"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();
        JsonNode ownership = json(ownershipResult);
        assertThat(ownership.get("sharePercentage").asText()).isEqualTo("60.0000");

        createTenancy(adminToken, community.getId(), unitId, tenant.getId(), LocalDate.now(), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.active").value(true));

        String ownerToken = accessToken(owner.getEmail());
        mockMvc.perform(get(ownershipsPath(community.getId(), unitId))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(owner.getId().toString()));

        String tenantToken = accessToken(tenant.getEmail());
        mockMvc.perform(get(tenanciesPath(community.getId(), unitId))
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(tenant.getId().toString()));

        createOwnership(ownerToken, community.getId(), unitId, owner.getId(),
                "10.0000", LocalDate.now(), null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PERMISSION_DENIED"));
    }

    @Test
    void ownershipRulesRejectDuplicatesAndExcessShares() throws Exception {
        AppUser admin = createUser(uniqueEmail("ownership-rules-admin"));
        AppUser owner1 = createUser(uniqueEmail("ownership-rules-1"));
        AppUser owner2 = createUser(uniqueEmail("ownership-rules-2"));
        Community community = createCommunity("Ownership Rules Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner1, "OWNER");
        createMembership(community, owner2, "OWNER");
        UUID unitId = createUnit(community, "1A");
        String token = accessToken(admin.getEmail());

        MvcResult first = createOwnership(token, community.getId(), unitId,
                owner1.getId(), "60.0000", LocalDate.now(), null)
                .andExpect(status().isCreated())
                .andReturn();
        UUID firstId = UUID.fromString(json(first).get("id").asText());

        createOwnership(token, community.getId(), unitId, owner1.getId(),
                "30.0000", LocalDate.now(), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OWNERSHIP_ALREADY_ACTIVE"));

        createOwnership(token, community.getId(), unitId, owner2.getId(),
                "50.0000", LocalDate.now(), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OWNERSHIP_SUM_EXCEEDS"));

        createOwnership(token, community.getId(), unitId, owner2.getId(),
                "40.0000", LocalDate.now(), null)
                .andExpect(status().isCreated());

        mockMvc.perform(delete(ownershipsPath(community.getId(), unitId) + "/" + firstId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        MvcResult listResult = mockMvc.perform(get(ownershipsPath(community.getId(), unitId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode list = json(listResult);
        assertThat(list.findValuesAsText("active")).contains("false", "true");

        createOwnership(token, community.getId(), unitId, owner1.getId(),
                "60.0000", LocalDate.now(), null)
                .andExpect(status().isCreated());
    }

    @Test
    void ownershipAndTenancyRequireActiveCommunityMember() throws Exception {
        AppUser admin = createUser(uniqueEmail("ownership-member-admin"));
        AppUser outsider = createUser(uniqueEmail("ownership-outsider"));
        Community community = createCommunity("Ownership Member Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        UUID unitId = createUnit(community, "1A");
        String token = accessToken(admin.getEmail());

        createOwnership(token, community.getId(), unitId, outsider.getId(),
                "10.0000", LocalDate.now(), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("OWNERSHIP_USER_NOT_MEMBER"));

        createTenancy(token, community.getId(), unitId, outsider.getId(),
                LocalDate.now(), null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("TENANCY_USER_NOT_MEMBER"));
    }

    @Test
    void crossCommunityOwnershipAccessIsRejected() throws Exception {
        AppUser userA = createUser(uniqueEmail("ownership-cross-a"));
        AppUser userB = createUser(uniqueEmail("ownership-cross-b"));
        Community communityA = createCommunity("Ownership Cross A");
        Community communityB = createCommunity("Ownership Cross B");
        createMembership(communityA, userA, "COMMUNITY_ADMIN");
        createMembership(communityB, userB, "COMMUNITY_ADMIN");
        UUID unitB = createUnit(communityB, "1A");
        String tokenA = accessToken(userA.getEmail());

        mockMvc.perform(get(ownershipsPath(communityB.getId(), unitB))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_FORBIDDEN"));
    }

    private ResultActions createOwnership(String token, UUID communityId, UUID unitId, UUID userId,
                                          String share, LocalDate validFrom, LocalDate validTo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId.toString());
        body.put("ownershipType", "OWNER");
        body.put("sharePercentage", share);
        body.put("validFrom", validFrom.toString());
        if (validTo != null) {
            body.put("validTo", validTo.toString());
        }
        return mockMvc.perform(post(ownershipsPath(communityId, unitId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions createTenancy(String token, UUID communityId, UUID unitId, UUID userId,
                                        LocalDate validFrom, LocalDate validTo) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId.toString());
        body.put("validFrom", validFrom.toString());
        if (validTo != null) {
            body.put("validTo", validTo.toString());
        }
        return mockMvc.perform(post(tenanciesPath(communityId, unitId))
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private String ownershipsPath(UUID communityId, UUID unitId) {
        return "/api/v1/communities/" + communityId + "/units/" + unitId + "/ownerships";
    }

    private String tenanciesPath(UUID communityId, UUID unitId) {
        return "/api/v1/communities/" + communityId + "/units/" + unitId + "/tenancies";
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
                "Ownership Tester",
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
