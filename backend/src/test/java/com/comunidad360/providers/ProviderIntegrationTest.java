package com.comunidad360.providers;

import com.comunidad360.community.Community;
import com.comunidad360.community.CommunityRepository;
import com.comunidad360.community.CommunityStatus;
import com.comunidad360.membership.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderIntegrationTest {
    @Container @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired CommunityRepository communityRepository;
    @Autowired MembershipRepository membershipRepository;
    @Autowired MembershipRoleRepository membershipRoleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void providerAndContactsRespectPermissions() throws Exception {
        AppUser admin = createUser(uniqueEmail("provider-admin"));
        AppUser secretary = createUser(uniqueEmail("provider-secretary"));
        Community community = createCommunity("Provider Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, secretary, "SECRETARY");
        String adminToken = accessToken(admin.getEmail());
        String secretaryToken = accessToken(secretary.getEmail());

        MvcResult created = mockMvc.perform(post(providersPath(community.getId()))
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Ascensores Norte",
                                "taxId", "B12345678",
                                "category", "MANTENIMIENTO",
                                "email", "INFO@ASCENSORES.EXAMPLE",
                                "phone", "600123123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("info@ascensores.example"))
                .andReturn();
        UUID providerId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(get(providersPath(community.getId()))
                        .header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(providerId.toString()));

        mockMvc.perform(post(providersPath(community.getId()))
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "No permitido"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(providersPath(community.getId()) + "/" + providerId + "/contacts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Ana Técnica",
                                "role", "Responsable",
                                "email", "ana@example.com",
                                "primaryContact", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.primaryContact").value(true));

        mockMvc.perform(get(providersPath(community.getId()) + "/" + providerId + "/contacts")
                        .header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Ana Técnica"));
    }

    @Test
    void providerCrossCommunityAccessIsRejected() throws Exception {
        AppUser adminA = createUser(uniqueEmail("provider-cross-a"));
        AppUser adminB = createUser(uniqueEmail("provider-cross-b"));
        Community communityA = createCommunity("Provider Cross A");
        Community communityB = createCommunity("Provider Cross B");
        createMembership(communityA, adminA, "COMMUNITY_ADMIN");
        createMembership(communityB, adminB, "COMMUNITY_ADMIN");
        String tokenA = accessToken(adminA.getEmail());
        String tokenB = accessToken(adminB.getEmail());
        MvcResult created = mockMvc.perform(post(providersPath(communityA.getId()))
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Proveedor A"))))
                .andExpect(status().isCreated()).andReturn();
        UUID providerId = UUID.fromString(json(created).get("id").asText());

        mockMvc.perform(get(providersPath(communityA.getId()) + "/" + providerId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isForbidden());
    }

    private String providersPath(UUID communityId) {
        return "/api/v1/communities/" + communityId + "/providers";
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
                "Provider Tester", "es", UserStatus.ACTIVE, now, now);
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
