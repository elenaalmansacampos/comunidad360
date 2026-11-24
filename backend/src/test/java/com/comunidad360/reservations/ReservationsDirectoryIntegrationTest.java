package com.comunidad360.reservations;

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
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReservationsDirectoryIntegrationTest {

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
    void reservationsAndDirectoryRespectPermissionsAndPrivacy() throws Exception {
        AppUser admin = createUser("res-admin");
        AppUser owner = createUser("res-owner");
        AppUser tenant = createUser("res-tenant");
        Community community = createCommunity("Reservations Community");
        createMembership(community, admin, "COMMUNITY_ADMIN");
        createMembership(community, owner, "OWNER");
        createMembership(community, tenant, "TENANT");
        String adminToken = accessToken(admin.getEmail());
        String ownerToken = accessToken(owner.getEmail());
        String tenantToken = accessToken(tenant.getEmail());
        String base = "/api/v1/communities/" + community.getId();

        MvcResult area = mockMvc.perform(post(base + "/common-areas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Salón de actos", "location", "Planta baja", "capacity", 40,
                                "requiresApproval", true))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresApproval").value(true))
                .andReturn();
        UUID areaId = UUID.fromString(json(area).get("id").asText());

        String date = LocalDate.now().plusDays(2).toString();
        MvcResult reservation = mockMvc.perform(post(base + "/reservations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "areaId", areaId, "date", date,
                                "startTime", "10:00:00", "endTime", "12:00:00",
                                "notes", "Cumpleaños"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.mine").value(true))
                .andReturn();
        UUID reservationId = UUID.fromString(json(reservation).get("id").asText());

        mockMvc.perform(post(base + "/reservations")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "areaId", areaId, "date", date,
                                "startTime", "11:00:00", "endTime", "13:00:00"))))
                .andExpect(status().isConflict());

        mockMvc.perform(patch(base + "/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(base + "/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "APPROVED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(delete(base + "/reservations/" + reservationId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(put(base + "/directory/me")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "displayName", "Ana Propietaria", "unitLabel", "2ºB",
                                "phone", "+34 600 111 111", "showEmail", false, "showPhone", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ana Propietaria"))
                .andExpect(jsonPath("$.unitLabel").value("2ºB"))
                .andExpect(jsonPath("$.phone").value("+34 600 111 111"));
        mockMvc.perform(put(base + "/directory/me")
                        .header("Authorization", "Bearer " + tenantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", "+34 600 222 222", "showEmail", true, "showPhone", false))))
                .andExpect(status().isOk());

        MvcResult directory = mockMvc.perform(get(base + "/directory")
                        .header("Authorization", "Bearer " + tenantToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ownerEntry = entry(directory, owner.getId());
        assertEquals("+34 600 111 111", ownerEntry.get("phone").asText());
        assertEquals("Ana Propietaria", ownerEntry.get("displayName").asText());
        assertEquals("2ºB", ownerEntry.get("unitLabel").asText());
        assertTrue(ownerEntry.get("email").isNull());
        assertFalse(ownerEntry.get("self").asBoolean());

        MvcResult ownerDirectory = mockMvc.perform(get(base + "/directory")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode selfEntry = entry(ownerDirectory, owner.getId());
        assertTrue(selfEntry.get("self").asBoolean());
        assertEquals(owner.getEmail(), selfEntry.get("email").asText());
        assertNotNull(selfEntry.get("phone").asText());
        JsonNode tenantEntry = entry(ownerDirectory, tenant.getId());
        assertNull(tenantEntry.get("phone").isNull() ? null : tenantEntry.get("phone").asText());
        assertNotNull(tenantEntry.get("email").asText());
    }

    private JsonNode entry(MvcResult result, UUID userId) throws Exception {
        for (JsonNode node : json(result)) {
            if (node.get("userId").asText().equals(userId.toString())) {
                return node;
            }
        }
        throw new AssertionError("directory entry not found");
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String accessToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Password123!"))))
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
