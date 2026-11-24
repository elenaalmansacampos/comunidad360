package com.comunidad360.votings.dto;

import java.util.UUID;

public record VotingDelegationResponse(UUID membershipId, UUID userId, String name, boolean hasVoted) {
}
