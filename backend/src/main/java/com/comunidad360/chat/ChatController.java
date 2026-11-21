package com.comunidad360.chat;

import com.comunidad360.chat.dto.*;
import com.comunidad360.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/chat")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/channels")
    public List<ChatChannelResponse> channels(@PathVariable UUID communityId,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.list(communityId, user.id());
    }

    @PostMapping("/channels")
    public ResponseEntity<ChatChannelResponse> create(@PathVariable UUID communityId,
                                                      @AuthenticationPrincipal AuthenticatedUser user,
                                                      @Valid @RequestBody CreateChatChannelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chatService.create(communityId, user.id(), request));
    }

    @GetMapping("/channels/{channelId}/messages")
    public List<ChatMessageResponse> messages(@PathVariable UUID communityId, @PathVariable UUID channelId,
                                              @AuthenticationPrincipal AuthenticatedUser user) {
        return chatService.messages(communityId, user.id(), channelId);
    }

    @PostMapping("/channels/{channelId}/messages")
    public ResponseEntity<ChatMessageResponse> send(@PathVariable UUID communityId, @PathVariable UUID channelId,
                                                    @AuthenticationPrincipal AuthenticatedUser user,
                                                    @Valid @RequestBody SendChatMessageRequest request) {
        ChatMessageResponse response = chatService.send(communityId, user.id(), channelId, request.body());
        messagingTemplate.convertAndSend(
                "/topic/communities/" + communityId + "/channels/" + channelId, response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
