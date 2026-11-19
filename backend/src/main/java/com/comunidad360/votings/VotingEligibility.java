package com.comunidad360.votings;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "voting_eligibility")
public class VotingEligibility {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "voting_id", nullable = false) private UUID votingId;
    @Column(name = "membership_id", nullable = false) private UUID membershipId;
    @Column(name = "coefficient_snapshot", nullable = false, precision = 9, scale = 6)
    private BigDecimal coefficientSnapshot;
    @Column(name = "has_voted", nullable = false) private boolean hasVoted;
    protected VotingEligibility() {}
    public VotingEligibility(UUID id, UUID communityId, UUID votingId, UUID membershipId,
                             BigDecimal coefficientSnapshot) {
        this.id = id; this.communityId = communityId; this.votingId = votingId;
        this.membershipId = membershipId; this.coefficientSnapshot = coefficientSnapshot;
        this.hasVoted = false;
    }
    public UUID getId() { return id; }
    public UUID getMembershipId() { return membershipId; }
    public BigDecimal getCoefficientSnapshot() { return coefficientSnapshot; }
    public boolean isHasVoted() { return hasVoted; }
    public void setHasVoted(boolean hasVoted) { this.hasVoted = hasVoted; }
}
