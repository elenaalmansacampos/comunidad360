package com.comunidad360.push;

import com.comunidad360.config.AppProperties;
import com.comunidad360.push.dto.PushSendSummary;
import com.comunidad360.push.dto.PushSubscriptionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PushSubscriptionServiceTest {

    private final PushSubscriptionRepository repository = mock(PushSubscriptionRepository.class);
    private final AppProperties properties = new AppProperties();

    @Test
    void sendsToEverySubscriptionAndRemovesGoneEndpoints() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        PushSubscription ok = subscription(userId, "https://push.example/ok", now);
        PushSubscription gone = subscription(userId, "https://push.example/gone", now);
        FakeTransport transport = new FakeTransport(true)
                .reply("https://push.example/ok", PushTransport.PushResult.success())
                .reply("https://push.example/gone", PushTransport.PushResult.goneResult());
        PushSubscriptionService service = service(transport);
        when(repository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(List.of(ok, gone));

        PushSendSummary summary = service.sendToUser(userId, "Título", "Cuerpo", "/x");

        assertThat(summary.sent()).isEqualTo(1);
        assertThat(summary.removed()).isEqualTo(1);
        assertThat(summary.failed()).isZero();
        assertThat(ok.getLastSuccessAt()).isNotNull();
        verify(repository).save(ok);
        verify(repository).delete(gone);
    }

    @Test
    void doesNothingWhenTransportIsNotConfigured() {
        FakeTransport transport = new FakeTransport(false);
        PushSubscriptionService service = service(transport);
        UUID userId = UUID.randomUUID();

        PushSendSummary summary = service.sendToUser(userId, "Título", "Cuerpo", "/x");

        assertThat(summary.attempted()).isZero();
        assertThat(transport.calls).isZero();
        verify(repository, never()).findByUserIdOrderByCreatedAtAsc(any());
    }

    @Test
    void subscribeCreatesNewSubscription() {
        PushSubscriptionService service = service(new FakeTransport(true));
        when(repository.findByEndpoint("https://push.example/new")).thenReturn(Optional.empty());
        UUID userId = UUID.randomUUID();

        service.subscribe(userId, request("https://push.example/new"), "JUnit");

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(subscription ->
                subscription.getUserId().equals(userId)
                        && subscription.getEndpoint().equals("https://push.example/new")
                        && subscription.getP256dh().equals("p256dh")
                        && subscription.getAuth().equals("auth")));
    }

    @Test
    void unsubscribeOnlyRemovesOwnedSubscriptions() {
        PushSubscriptionService service = service(new FakeTransport(true));
        UUID owner = UUID.randomUUID();
        PushSubscription subscription = subscription(UUID.randomUUID(), "https://push.example/x", Instant.now());
        when(repository.findByEndpoint("https://push.example/x")).thenReturn(Optional.of(subscription));

        service.unsubscribe(owner, "https://push.example/x");

        verify(repository, never()).delete(any());
    }

    private PushSubscriptionService service(PushTransport transport) {
        return new PushSubscriptionService(repository, transport, new ObjectMapper(), properties);
    }

    private PushSubscription subscription(UUID userId, String endpoint, Instant now) {
        return new PushSubscription(UUID.randomUUID(), userId, endpoint, "p256dh", "auth", "UA", now, now);
    }

    private PushSubscriptionRequest request(String endpoint) {
        return new PushSubscriptionRequest(endpoint, new PushSubscriptionRequest.Keys("p256dh", "auth"));
    }

    private static final class FakeTransport implements PushTransport {

        private final boolean configured;
        private final java.util.Map<String, PushResult> results = new java.util.HashMap<>();
        private int calls;

        private FakeTransport(boolean configured) {
            this.configured = configured;
        }

        private FakeTransport reply(String endpoint, PushResult result) {
            results.put(endpoint, result);
            return this;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public PushResult send(String endpoint, String p256dh, String auth, byte[] payload) {
            calls++;
            return results.getOrDefault(endpoint, PushResult.failure("PUSH_TEST_FAILED"));
        }
    }
}
