package com.comunidad360.chat;

import com.comunidad360.common.exception.BusinessException;
import com.comunidad360.chat.dto.*;
import com.comunidad360.membership.Membership;
import com.comunidad360.membership.MembershipRepository;
import com.comunidad360.membership.MembershipStatus;
import com.comunidad360.membership.PermissionService;
import com.comunidad360.membership.TenantAccessService;
import com.comunidad360.user.AppUser;
import com.comunidad360.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class ChatService {
    private final ChatChannelRepository channelRepository;
    private final ChatChannelMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final MembershipRepository membershipRepository;
    private final TenantAccessService tenantAccessService;
    private final PermissionService permissionService;
    private final AppUserRepository userRepository;

    public ChatService(ChatChannelRepository channelRepository, ChatChannelMemberRepository memberRepository,
                       ChatMessageRepository messageRepository, MembershipRepository membershipRepository,
                       TenantAccessService tenantAccessService, PermissionService permissionService,
                       AppUserRepository userRepository) {
        this.channelRepository = channelRepository;
        this.memberRepository = memberRepository;
        this.messageRepository = messageRepository;
        this.membershipRepository = membershipRepository;
        this.tenantAccessService = tenantAccessService;
        this.permissionService = permissionService;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<ChatChannelResponse> list(UUID communityId, UUID userId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "CHAT_VIEW");
        ensureGeneral(communityId, userId);
        return channelRepository.findByCommunityIdAndArchivedAtIsNullOrderByTypeAscNameAsc(communityId).stream()
                .filter(channel -> canAccess(channel, membership))
                .map(ChatChannelResponse::from).toList();
    }

    @Transactional
    public ChatChannelResponse create(UUID communityId, UUID userId, CreateChatChannelRequest request) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "CHAT_MODERATE");
        if (request.type() == ChatChannelType.GENERAL) {
            throw BusinessException.unprocessable("CHAT_CHANNEL_INVALID", "errors.chat.channelInvalid");
        }
        Instant now = Instant.now();
        ChatChannel channel = channelRepository.saveAndFlush(new ChatChannel(
                UUID.randomUUID(), communityId, request.type(), request.name().trim(),
                clean(request.description()), userId, now, now));
        if (request.type() == ChatChannelType.PRIVATE) {
            Set<UUID> ids = new LinkedHashSet<>();
            ids.add(membership.getId());
            if (request.memberMembershipIds() != null) ids.addAll(request.memberMembershipIds());
            for (UUID memberId : ids) {
                membershipRepository.findById(memberId)
                        .filter(value -> value.getCommunityId().equals(communityId))
                        .filter(value -> value.getStatus() == MembershipStatus.ACTIVE)
                        .orElseThrow(() -> BusinessException.unprocessable(
                                "CHAT_MEMBER_INVALID", "errors.chat.memberInvalid"));
                memberRepository.save(new ChatChannelMember(UUID.randomUUID(), communityId, channel.getId(),
                        memberId, memberId.equals(membership.getId()) ? "MODERATOR" : "MEMBER", now));
            }
            memberRepository.flush();
        }
        return ChatChannelResponse.from(channel);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> messages(UUID communityId, UUID userId, UUID channelId) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "CHAT_VIEW");
        ChatChannel channel = channelRepository.findByIdAndCommunityId(channelId, communityId)
                .orElseThrow(() -> BusinessException.notFound("CHAT_CHANNEL_NOT_FOUND", "errors.resource.notFound"));
        if (!canAccess(channel, membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        List<ChatMessage> messages = new ArrayList<>(messageRepository
                .findTop100ByCommunityIdAndChannelIdOrderByCreatedAtDesc(communityId, channelId));
        Collections.reverse(messages);
        return messages.stream().map(message -> ChatMessageResponse.from(message, userName(message.getSenderUserId()))).toList();
    }

    @Transactional
    public ChatMessageResponse send(UUID communityId, UUID userId, UUID channelId, String body) {
        Membership membership = tenantAccessService.requireActiveMembership(communityId, userId);
        permissionService.require(membership, "CHAT_VIEW");
        ChatChannel channel = channelRepository.findByIdAndCommunityId(channelId, communityId)
                .orElseThrow(() -> BusinessException.notFound("CHAT_CHANNEL_NOT_FOUND", "errors.resource.notFound"));
        if (!canAccess(channel, membership)) {
            throw BusinessException.forbidden("PERMISSION_DENIED", "errors.permission.denied");
        }
        ChatMessage message = messageRepository.saveAndFlush(new ChatMessage(
                UUID.randomUUID(), communityId, channelId, userId, body.trim(), Instant.now()));
        return ChatMessageResponse.from(message, userName(userId));
    }

    private void ensureGeneral(UUID communityId, UUID userId) {
        if (channelRepository.findFirstByCommunityIdAndTypeAndArchivedAtIsNull(
                communityId, ChatChannelType.GENERAL).isPresent()) {
            return;
        }
        Instant now = Instant.now();
        channelRepository.saveAndFlush(new ChatChannel(UUID.randomUUID(), communityId,
                ChatChannelType.GENERAL, "General", "Canal general de la comunidad", userId, now, now));
    }

    private boolean canAccess(ChatChannel channel, Membership membership) {
        if (channel.getType() != ChatChannelType.PRIVATE) return true;
        return memberRepository.findByChannelIdAndMembershipId(channel.getId(), membership.getId()).isPresent();
    }

    private String userName(UUID userId) {
        return userRepository.findById(userId).map(this::name).orElse(null);
    }

    private String name(AppUser user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
