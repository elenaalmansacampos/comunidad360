package com.comunidad360.chat;

import com.comunidad360.security.AuthenticatedUser;
import com.comunidad360.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ChatWebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String SESSION_USER = "chatUser";
    private static final Pattern TOPIC = Pattern.compile(
            "^/topic/communities/([0-9a-fA-F-]{36})/channels/([0-9a-fA-F-]{36})$");
    private static final Pattern APP_SEND = Pattern.compile(
            "^/app/communities/([0-9a-fA-F-]{36})/channels/([0-9a-fA-F-]{36})/send$");

    private final JwtService jwtService;
    private final ChatService chatService;

    public ChatWebSocketAuthInterceptor(JwtService jwtService, ChatService chatService) {
        this.jwtService = jwtService;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            connect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            Destination destination = parse(accessor.getDestination(), TOPIC);
            if (destination != null) {
                requireAccess(accessor, destination);
            }
        } else if (StompCommand.SEND.equals(accessor.getCommand())) {
            Destination destination = parse(accessor.getDestination(), APP_SEND);
            if (destination != null) {
                requireAccess(accessor, destination);
            }
        }
        return message;
    }

    private void connect(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null) {
            authorization = accessor.getFirstNativeHeader("authorization");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new MessagingException("Missing token");
        }
        try {
            Claims claims = jwtService.parse(authorization.substring(7));
            AuthenticatedUser user = new AuthenticatedUser(
                    UUID.fromString(claims.getSubject()),
                    claims.get("email", String.class),
                    claims.get("name", String.class),
                    claims.get("lang", String.class));
            accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            Map<String, Object> attributes = accessor.getSessionAttributes();
            if (attributes != null) {
                attributes.put(SESSION_USER, user);
            }
        } catch (Exception exception) {
            throw new MessagingException("Invalid token");
        }
    }

    private void requireAccess(StompHeaderAccessor accessor, Destination destination) {
        AuthenticatedUser user = user(accessor);
        if (user == null) {
            throw new MessagingException("Unauthorized");
        }
        try {
            chatService.requireChannelAccess(destination.communityId(), user.id(), destination.channelId());
        } catch (Exception exception) {
            throw new MessagingException("Forbidden");
        }
    }

    private AuthenticatedUser user(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        if (attributes != null && attributes.get(SESSION_USER) instanceof AuthenticatedUser user) {
            return user;
        }
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        return null;
    }

    private Destination parse(String destination, Pattern pattern) {
        if (destination == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new Destination(UUID.fromString(matcher.group(1)), UUID.fromString(matcher.group(2)));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record Destination(UUID communityId, UUID channelId) {
    }
}
