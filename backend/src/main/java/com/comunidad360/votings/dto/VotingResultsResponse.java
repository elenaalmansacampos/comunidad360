package com.comunidad360.votings.dto;

import java.math.BigDecimal;
import java.util.List;

public record VotingResultsResponse(
        long totalEligible, long participatingEligible, BigDecimal totalCoefficient,
        BigDecimal participatingCoefficient, BigDecimal quorumPercentage,
        List<VotingOptionResultResponse> options
) {}
