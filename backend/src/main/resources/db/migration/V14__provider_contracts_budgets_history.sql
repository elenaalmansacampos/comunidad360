CREATE TABLE provider_contract (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    start_date DATE,
    end_date DATE,
    amount NUMERIC(14, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT provider_contract_provider_fk
        FOREIGN KEY (provider_id, community_id) REFERENCES provider (id, community_id),
    CONSTRAINT provider_contract_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT provider_contract_status_check
        CHECK (status IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX provider_contract_provider_idx
    ON provider_contract (provider_id, created_at DESC);

CREATE TABLE provider_budget (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    amount NUMERIC(14, 2) NOT NULL,
    valid_until DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT provider_budget_provider_fk
        FOREIGN KEY (provider_id, community_id) REFERENCES provider (id, community_id),
    CONSTRAINT provider_budget_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT provider_budget_status_check
        CHECK (status IN ('RECEIVED', 'ACCEPTED', 'REJECTED', 'EXPIRED'))
);

CREATE INDEX provider_budget_provider_idx
    ON provider_budget (provider_id, created_at DESC);

CREATE TABLE provider_note (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    body VARCHAR(2000) NOT NULL,
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT provider_note_provider_fk
        FOREIGN KEY (provider_id, community_id) REFERENCES provider (id, community_id),
    CONSTRAINT provider_note_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX provider_note_provider_idx
    ON provider_note (provider_id, created_at DESC);
