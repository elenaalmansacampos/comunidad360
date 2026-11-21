package com.comunidad360.chat;

import com.comunidad360.config.AppProperties;
import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.security.JwtService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChatWebSocketAuthInterceptorTest {

    private final JwtService jwtService = jwtService();

    @Test
    void connectWithBearerTokenStoresAuthenticatedUser() {
        AppUser user = user("ana@example.com");
        String token = jwtService.createAccessToken(user);
        ChatWebSocketAuthInterceptor interceptor =
                new ChatWebSocketAuthInterceptor(jwtService, new RecordingChatService(false));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Map<String, Object> attributes = new HashMap<>();
        accessor.setSessionAttributes(attributes);

        interceptor.preSend(message(accessor), null);

        assertThat(accessor.getUser()).isNotNull();
        assertThat(attributes.get("chatUser")).isInstanceOf(AuthenticatedUser.class);
        assertThat(((AuthenticatedUser) attributes.get("chatUser")).id()).isEqualTo(user.getId());
    }

    @Test
    void connectWithoutTokenIsRejected() {
        ChatWebSocketAuthInterceptor interceptor =
                new ChatWebSocketAuthInterceptor(jwtService, new RecordingChatService(false));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(new HashMap<>());

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Missing token");
    }

    @Test
    void subscribeRequiresChannelAccess() {
        UUID communityId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        ChatWebSocketAuthInterceptor interceptor =
                new ChatWebSocketAuthInterceptor(jwtService, new RecordingChatService(true));
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/communities/" + communityId + "/channels/" + channelId);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("chatUser", new AuthenticatedUser(UUID.randomUUID(), "ana@example.com", "Ana", "es"));
        accessor.setSessionAttributes(attributes);

        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(MessagingException.class)
                .hasMessageContaining("Forbidden");
    }

    @Test
    void subscribeToOwnChannelIsAllowed() {
        UUID communityId = UUID.randomUUID();
        UUID channelId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RecordingChatService chatService = new RecordingChatService(false);
        ChatWebSocketAuthInterceptor interceptor =
                new ChatWebSocketAuthInterceptor(jwtService, chatService);
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/communities/" + communityId + "/channels/" + channelId);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("chatUser", new AuthenticatedUser(userId, "ana@example.com", "Ana", "es"));
        accessor.setSessionAttributes(attributes);

        interceptor.preSend(message(accessor), null);

        assertThat(chatService.communityId).isEqualTo(communityId);
        assertThat(chatService.userId).isEqualTo(userId);
        assertThat(chatService.channelId).isEqualTo(channelId);
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private JwtService jwtService() {
        AppProperties properties = new AppProperties();
        properties.getSecurity().setJwtSecret("test-only-secret-0123456789-abcdefghijklmnopqrstuvwxyz");
        return new JwtService(properties);
    }

    private AppUser user(String email) {
        Instant now = Instant.now();
        return new AppUser(UUID.randomUUID(), email, "hash", "Ana", "es", UserStatus.ACTIVE, now, now);
    }

    private static final class RecordingChatService extends ChatService {

        private final boolean forbidden;
        private UUID communityId;
        private UUID userId;
        private UUID channelId;

        private RecordingChatService(boolean forbidden) {
            super(null, null, null, null, null, null, null);
            this.forbidden = forbidden;
        }

        @Override
        public void requireChannelAccess(UUID communityId, UUID userId, UUID channelId) {
            if (forbidden) {
                throw new RuntimeException("forbidden");
            }
            this.communityId = communityId;
            this.userId = userId;
            this.channelId = channelId;
        }
    }
}
