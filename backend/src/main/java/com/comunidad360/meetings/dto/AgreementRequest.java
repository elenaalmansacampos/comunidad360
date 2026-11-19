package com.comunidad360.meetings.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AgreementRequest(
        UUID agendaItemId,
        @NotBlank @Size(max = 300) String title,
        @Size(max = 2000) String description,
        @Min(0) int votesFor,
        @Min(0) int votesAgainst,
        @Min(0) int abstentions,
        BigDecimal coefficientFor,
        BigDecimal coefficientAgainst,
        BigDecimal coefficientAbstention,
        Boolean adopted
) {
}
