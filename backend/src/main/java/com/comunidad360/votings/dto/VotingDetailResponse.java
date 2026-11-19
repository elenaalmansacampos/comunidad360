package com.comunidad360.votings.dto;

import java.util.List;

public record VotingDetailResponse(
        VotingResponse voting, List<VotingOptionResponse> options, VotingResultsResponse results
) {}
