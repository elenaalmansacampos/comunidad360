package com.comunidad360.meetings.dto;

import com.comunidad360.meetings.MeetingAgreement;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AgreementResponse(
        UUID id,
        UUID agendaItemId,
        String title,
        String description,
        Integer votesFor,
        Integer votesAgainst,
        Integer abstentions,
        BigDecimal coefficientFor,
        BigDecimal coefficientAgainst,
        BigDecimal coefficientAbstention,
        Boolean adopted,
        Instant createdAt
) {
    public static AgreementResponse from(MeetingAgreement agreement) {
        return new AgreementResponse(
                agreement.getId(),
                agreement.getAgendaItemId(),
                agreement.getTitle(),
                agreement.getDescription(),
                agreement.getVotesFor(),
                agreement.getVotesAgainst(),
                agreement.getAbstentions(),
                agreement.getCoefficientFor(),
                agreement.getCoefficientAgainst(),
                agreement.getCoefficientAbstention(),
                agreement.getAdopted(),
                agreement.getCreatedAt()
        );
    }
}
