CREATE TABLE voting (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    opens_at TIMESTAMPTZ,
    closes_at TIMESTAMPTZ,
    legal_notice VARCHAR(2000) NOT NULL,
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT voting_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT voting_status_check CHECK (status IN ('DRAFT', 'OPEN', 'CLOSED', 'CANCELLED')),
    CONSTRAINT voting_dates_check CHECK (closes_at IS NULL OR opens_at IS NULL OR closes_at > opens_at)
);

CREATE INDEX voting_community_idx ON voting (community_id, status, closes_at);

CREATE TABLE voting_option (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    voting_id UUID NOT NULL,
    label VARCHAR(200) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT voting_option_voting_fk
        FOREIGN KEY (voting_id, community_id) REFERENCES voting (id, community_id),
    CONSTRAINT voting_option_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT voting_option_unique UNIQUE (voting_id, position)
);

CREATE TABLE voting_eligibility (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    voting_id UUID NOT NULL,
    membership_id UUID NOT NULL REFERENCES membership (id),
    coefficient_snapshot NUMERIC(9,6) NOT NULL DEFAULT 0,
    has_voted BOOLEAN NOT NULL DEFAULT false,
    CONSTRAINT voting_eligibility_voting_fk
        FOREIGN KEY (voting_id, community_id) REFERENCES voting (id, community_id),
    CONSTRAINT voting_eligibility_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT voting_eligibility_unique UNIQUE (voting_id, membership_id)
);

CREATE TABLE vote (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    voting_id UUID NOT NULL,
    eligibility_id UUID NOT NULL,
    option_id UUID NOT NULL,
    cast_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT vote_voting_fk
        FOREIGN KEY (voting_id, community_id) REFERENCES voting (id, community_id),
    CONSTRAINT vote_eligibility_fk
        FOREIGN KEY (eligibility_id, community_id) REFERENCES voting_eligibility (id, community_id),
    CONSTRAINT vote_option_fk
        FOREIGN KEY (option_id, community_id) REFERENCES voting_option (id, community_id),
    CONSTRAINT vote_unique UNIQUE (voting_id, eligibility_id)
);
