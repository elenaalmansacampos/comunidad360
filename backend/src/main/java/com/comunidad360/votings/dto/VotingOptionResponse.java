package com.comunidad360.votings.dto;

import com.comunidad360.votings.VotingOption;
import java.util.UUID;

public record VotingOptionResponse(UUID id, String label, Integer position) {
    public static VotingOptionResponse from(VotingOption option) {
        return new VotingOptionResponse(option.getId(), option.getLabel(), option.getPosition());
    }
}
