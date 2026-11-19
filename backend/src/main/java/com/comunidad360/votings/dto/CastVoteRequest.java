package com.comunidad360.votings.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CastVoteRequest(@NotNull UUID optionId) {}
