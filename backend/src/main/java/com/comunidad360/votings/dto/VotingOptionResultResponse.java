package com.comunidad360.votings.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VotingOptionResultResponse(
        UUID optionId, String label, long votes, BigDecimal coefficient, BigDecimal percentage
) {}
