package com.comunidad360.push;

import com.comunidad360.config.AppProperties;
import com.comunidad360.push.dto.PushSendSummary;
import com.comunidad360.push.dto.PushSubscriptionRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushSubscriptionService {

    private final PushSubscriptionRepository repository;
    private final PushTransport transport;
    private final ObjectMapper objectMapper;
    private final AppProperties properties;

    public PushSubscriptionService(PushSubscriptionRepository repository, PushTransport transport,
                                   ObjectMapper objectMapper, AppProperties properties) {
        this.repository = repository;
        this.transport = transport;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public boolean isConfigured() {
        return transport.isConfigured();
    }

    public String publicKey() {
        return transport.isConfigured() ? properties.getPush().getPublicKey() : null;
    }

    @Transactional
    public void subscribe(UUID userId, PushSubscriptionRequest request, String userAgent) {
        Instant now = Instant.now();
        String endpoint = request.endpoint().trim();
        PushSubscription subscription = repository.findByEndpoint(endpoint).orElse(null);
        if (subscription == null) {
            subscription = new PushSubscription(UUID.randomUUID(), userId, endpoint,
                    request.keys().p256dh(), request.keys().auth(), truncate(userAgent, 500), now, now);
        } else {
            subscription.setUserId(userId);
            subscription.setP256dh(request.keys().p256dh());
            subscription.setAuth(request.keys().auth());
            subscription.setUserAgent(truncate(userAgent, 500));
            subscription.setUpdatedAt(now);
            subscription.setFailureCount(0);
        }
        repository.save(subscription);
    }

    @Transactional
    public void unsubscribe(UUID userId, String endpoint) {
        repository.findByEndpoint(endpoint.trim())
                .filter(subscription -> subscription.getUserId().equals(userId))
                .ifPresent(repository::delete);
    }

    @Transactional
    public PushSendSummary sendToUser(UUID userId, String title, String body, String url) {
        if (!transport.isConfigured()) {
            return PushSendSummary.empty();
        }
        List<PushSubscription> subscriptions = repository.findByUserIdOrderByCreatedAtAsc(userId);
        if (subscriptions.isEmpty()) {
            return PushSendSummary.empty();
        }
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(Map.of(
                    "title", clip(title == null ? "Comunidad 360" : title, 80),
                    "body", clip(body == null ? "" : body, 240),
                    "url", url == null ? "/" : url
            ));
        } catch (JsonProcessingException exception) {
            return PushSendSummary.empty();
        }
        int sent = 0;
        int failed = 0;
        int removed = 0;
        Instant now = Instant.now();
        for (PushSubscription subscription : subscriptions) {
            PushTransport.PushResult result = transport.send(
                    subscription.getEndpoint(), subscription.getP256dh(), subscription.getAuth(), payload);
            if (result.gone()) {
                removed++;
                repository.delete(subscription);
                continue;
            }
            if (result.sent()) {
                sent++;
                subscription.setLastSuccessAt(now);
                subscription.setFailureCount(0);
            } else {
                failed++;
                subscription.setFailureCount(subscription.getFailureCount() + 1);
            }
            repository.save(subscription);
        }
        return new PushSendSummary(sent, failed, removed);
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String clip(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
