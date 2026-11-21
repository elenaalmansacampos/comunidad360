package com.comunidad360.chat;

import com.comunidad360.chat.dto.ChatMessageResponse;
import com.comunidad360.chat.dto.ChatSocketMessage;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/communities/{communityId}/channels/{channelId}/send")
    public void send(@DestinationVariable UUID communityId, @DestinationVariable UUID channelId,
                     @Valid @Payload ChatSocketMessage request, Principal principal) {
        AuthenticatedUser user = user(principal);
        ChatMessageResponse response = chatService.send(communityId, user.id(), channelId, request.body());
        messagingTemplate.convertAndSend(topic(communityId, channelId), response);
    }

    private AuthenticatedUser user(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new MessagingException("Unauthorized");
    }

    private String topic(UUID communityId, UUID channelId) {
        return "/topic/communities/" + communityId + "/channels/" + channelId;
    }
}
