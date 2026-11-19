CREATE TABLE property_ownership (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user (id),
    ownership_type VARCHAR(20) NOT NULL,
    share_percentage NUMERIC(7,4) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ownership_type_check CHECK (ownership_type IN ('OWNER', 'CO_OWNER', 'USUFRUCTUARY', 'NAKED_OWNER')),
    CONSTRAINT ownership_share_check CHECK (share_percentage > 0 AND share_percentage <= 100),
    CONSTRAINT ownership_dates_check CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT ownership_unit_community_fk
        FOREIGN KEY (unit_id, community_id) REFERENCES unit (id, community_id)
);

CREATE INDEX ownership_unit_idx ON property_ownership (unit_id, valid_from DESC);
CREATE INDEX ownership_user_idx ON property_ownership (community_id, user_id, valid_from DESC);
CREATE UNIQUE INDEX ownership_active_unique
    ON property_ownership (unit_id, user_id) WHERE valid_to IS NULL;

CREATE TABLE tenancy (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    unit_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES app_user (id),
    valid_from DATE NOT NULL,
    valid_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    authorized_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT tenancy_status_check CHECK (status IN ('ACTIVE', 'ENDED', 'REVOKED')),
    CONSTRAINT tenancy_dates_check CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT tenancy_unit_community_fk
        FOREIGN KEY (unit_id, community_id) REFERENCES unit (id, community_id)
);

CREATE INDEX tenancy_unit_idx ON tenancy (unit_id, valid_from DESC);
CREATE INDEX tenancy_user_idx ON tenancy (community_id, user_id, valid_from DESC);
CREATE UNIQUE INDEX tenancy_active_unique
    ON tenancy (unit_id, user_id) WHERE status = 'ACTIVE';
