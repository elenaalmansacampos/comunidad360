package com.comunidad360.votings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateVotingRequest(
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        Instant opensAt,
        @NotNull Instant closesAt,
        @NotNull @Size(min = 2, max = 10) List<@NotBlank @Size(max = 200) String> options
) {}
