CREATE TABLE organization (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT organization_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX organization_name_idx ON organization (name);

CREATE TABLE organization_member (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organization (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    role_code VARCHAR(20) NOT NULL DEFAULT 'ORG_ADMIN',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT organization_member_unique UNIQUE (organization_id, user_id),
    CONSTRAINT organization_member_role_check CHECK (role_code IN ('ORG_ADMIN', 'ORG_STAFF')),
    CONSTRAINT organization_member_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX organization_member_user_idx ON organization_member (user_id);

ALTER TABLE community
    ADD COLUMN organization_id UUID REFERENCES organization (id) ON DELETE SET NULL;

CREATE INDEX community_organization_idx ON community (organization_id);

ALTER TABLE membership
    ADD COLUMN source_organization_id UUID REFERENCES organization (id) ON DELETE SET NULL;
