CREATE TABLE provider (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(20),
    category VARCHAR(100),
    email VARCHAR(320),
    phone VARCHAR(50),
    address VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(2000),
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT provider_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT provider_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX provider_community_idx ON provider (community_id, status, name);

CREATE TABLE provider_contact (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    role VARCHAR(100),
    email VARCHAR(320),
    phone VARCHAR(50),
    is_primary BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT provider_contact_provider_fk
        FOREIGN KEY (provider_id, community_id) REFERENCES provider (id, community_id),
    CONSTRAINT provider_contact_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX provider_contact_provider_idx ON provider_contact (provider_id, is_primary DESC, name);
