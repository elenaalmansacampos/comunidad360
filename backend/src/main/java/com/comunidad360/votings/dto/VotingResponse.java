package com.comunidad360.votings.dto;

import com.comunidad360.votings.Voting;
import com.comunidad360.votings.VotingStatus;
import java.time.Instant;
import java.util.UUID;

public record VotingResponse(
        UUID id, String title, String description, VotingStatus status, Instant opensAt,
        Instant closesAt, String legalNotice, long totalEligible, boolean hasVoted,
        Instant createdAt, long version
) {
    public static VotingResponse from(Voting voting, long totalEligible, boolean hasVoted) {
        return new VotingResponse(voting.getId(), voting.getTitle(), voting.getDescription(),
                voting.getStatus(), voting.getOpensAt(), voting.getClosesAt(), voting.getLegalNotice(),
                totalEligible, hasVoted, voting.getCreatedAt(), voting.getVersion());
    }
}
