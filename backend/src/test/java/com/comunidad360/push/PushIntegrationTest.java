package com.comunidad360.push;

import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import com.comunidad360.user.UserStatus;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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
class PushIntegrationTest {

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
    PushSubscriptionRepository pushRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void subscriptionLifecycleAndDisabledTransport() throws Exception {
        AppUser user = createUser("push-" + UUID.randomUUID() + "@example.com");
        String token = accessToken(user.getEmail());

        mockMvc.perform(get("/api/v1/me/push/public-key")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        String endpoint = "https://push.example/" + UUID.randomUUID();
        String body = objectMapper.writeValueAsString(Map.of(
                "endpoint", endpoint,
                "keys", Map.of("p256dh", "p256dh-key", "auth", "auth-key")));
        mockMvc.perform(post("/api/v1/me/push/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        assertThat(pushRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).hasSize(1);

        mockMvc.perform(delete("/api/v1/me/push/subscriptions")
                        .header("Authorization", "Bearer " + token)
                        .param("endpoint", endpoint))
                .andExpect(status().isNoContent());

        assertThat(pushRepository.findByUserIdOrderByCreatedAtAsc(user.getId())).isEmpty();

        mockMvc.perform(post("/api/v1/me/push/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(0))
                .andExpect(jsonPath("$.failed").value(0));
    }

    private String accessToken(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private AppUser createUser(String email) {
        Instant now = Instant.now();
        AppUser user = new AppUser(UUID.randomUUID(), email, passwordEncoder.encode("Password123!"),
                "Push Tester", "es", UserStatus.ACTIVE, now, now);
        user.setEmailVerifiedAt(now);
        return userRepository.save(user);
    }
}
