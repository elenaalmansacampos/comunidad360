package com.comunidad360.organization;

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
class OrganizationIntegrationTest {

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
    void organizationGroupsCommunitiesAndPropagatesAdmins() throws Exception {
        AppUser admin = createUser(uniqueEmail("org-admin"));
        AppUser staff = createUser(uniqueEmail("org-staff"));
        AppUser outsider = createUser(uniqueEmail("org-outsider"));
        Community community = createCommunity("Org Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        String adminToken = accessToken(admin.getEmail());
        String staffToken = accessToken(staff.getEmail());
        String outsiderToken = accessToken(outsider.getEmail());

        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Administraciones Ejemplo",
                                "taxId", "B12345678"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"))
                .andReturn();
        UUID organizationId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/communities/" + community.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", staff.getEmail(),
                                "role", "ORG_ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ORG_ADMIN"));

        Membership staffMembership = membershipRepository
                .findByCommunityIdAndUserId(community.getId(), staff.getId())
                .orElseThrow();
        assertThat(staffMembership.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
        assertThat(staffMembership.getSourceOrganizationId()).isEqualTo(organizationId);
        assertThat(membershipRoleRepository.findRoleCodesByMembershipId(staffMembership.getId()))
                .contains("COMMUNITY_ADMIN");

        mockMvc.perform(get("/api/v1/organizations/mine")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(organizationId.toString()))
                .andExpect(jsonPath("$[0].role").value("ORG_ADMIN"));

        mockMvc.perform(get("/api/v1/organizations/" + organizationId)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/organizations/" + organizationId + "/members/" + staff.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(membershipRepository.findByCommunityIdAndUserId(community.getId(), staff.getId())
                .orElseThrow().getStatus()).isEqualTo(MembershipStatus.REMOVED);

        mockMvc.perform(delete("/api/v1/organizations/" + organizationId + "/communities/" + community.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(communityRepository.findById(community.getId()).orElseThrow().getOrganizationId()).isNull();
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

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                "Organization Tester", "es", UserStatus.ACTIVE, now, now);
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
