package com.comunidad360.chat.dto;

import com.comunidad360.chat.ChatChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateChatChannelRequest(
        @NotNull ChatChannelType type,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        List<UUID> memberMembershipIds
) {}
