package com.comunidad360.votings;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vote")
public class Vote {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "voting_id", nullable = false) private UUID votingId;
    @Column(name = "eligibility_id", nullable = false) private UUID eligibilityId;
    @Column(name = "option_id", nullable = false) private UUID optionId;
    @Column(name = "cast_at", nullable = false) private Instant castAt;
    protected Vote() {}
    public Vote(UUID id, UUID communityId, UUID votingId, UUID eligibilityId, UUID optionId,
                Instant castAt) {
        this.id = id; this.communityId = communityId; this.votingId = votingId;
        this.eligibilityId = eligibilityId; this.optionId = optionId; this.castAt = castAt;
    }
    public UUID getOptionId() { return optionId; }
    public UUID getEligibilityId() { return eligibilityId; }
}
