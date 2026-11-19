package com.comunidad360.meetings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "meeting_agreement")
public class MeetingAgreement {

    @Id
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "meeting_id", nullable = false)
    private UUID meetingId;

    @Column(name = "agenda_item_id")
    private UUID agendaItemId;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(name = "votes_for", nullable = false)
    private Integer votesFor;

    @Column(name = "votes_against", nullable = false)
    private Integer votesAgainst;

    @Column(nullable = false)
    private Integer abstentions;

    @Column(name = "coefficient_for", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientFor;

    @Column(name = "coefficient_against", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientAgainst;

    @Column(name = "coefficient_abstention", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientAbstention;

    @Column
    private Boolean adopted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MeetingAgreement() {
    }

    public MeetingAgreement(UUID id, UUID communityId, UUID meetingId, UUID agendaItemId,
                            String title, String description, Integer votesFor, Integer votesAgainst,
                            Integer abstentions, BigDecimal coefficientFor, BigDecimal coefficientAgainst,
                            BigDecimal coefficientAbstention, Boolean adopted, Instant createdAt) {
        this.id = id;
        this.communityId = communityId;
        this.meetingId = meetingId;
        this.agendaItemId = agendaItemId;
        this.title = title;
        this.description = description;
        this.votesFor = votesFor;
        this.votesAgainst = votesAgainst;
        this.abstentions = abstentions;
        this.coefficientFor = coefficientFor;
        this.coefficientAgainst = coefficientAgainst;
        this.coefficientAbstention = coefficientAbstention;
        this.adopted = adopted;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgendaItemId() {
        return agendaItemId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getVotesFor() {
        return votesFor;
    }

    public Integer getVotesAgainst() {
        return votesAgainst;
    }

    public Integer getAbstentions() {
        return abstentions;
    }

    public BigDecimal getCoefficientFor() {
        return coefficientFor;
    }

    public BigDecimal getCoefficientAgainst() {
        return coefficientAgainst;
    }

    public BigDecimal getCoefficientAbstention() {
        return coefficientAbstention;
    }

    public Boolean getAdopted() {
        return adopted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
