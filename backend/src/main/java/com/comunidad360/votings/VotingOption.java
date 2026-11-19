package com.comunidad360.votings;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "voting_option")
public class VotingOption {
    @Id private UUID id;
    @Column(name = "community_id", nullable = false) private UUID communityId;
    @Column(name = "voting_id", nullable = false) private UUID votingId;
    @Column(nullable = false, length = 200) private String label;
    @Column(nullable = false) private Integer position;
    protected VotingOption() {}
    public VotingOption(UUID id, UUID communityId, UUID votingId, String label, Integer position) {
        this.id = id; this.communityId = communityId; this.votingId = votingId;
        this.label = label; this.position = position;
    }
    public UUID getId() { return id; }
    public UUID getVotingId() { return votingId; }
    public String getLabel() { return label; }
    public Integer getPosition() { return position; }
}
