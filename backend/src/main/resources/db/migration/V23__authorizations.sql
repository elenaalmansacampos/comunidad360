INSERT INTO permission (code, description_key, module) VALUES
    ('AUTHORIZATION_VIEW', 'permission.authorizationView', 'authorizations'),
    ('AUTHORIZATION_CREATE', 'permission.authorizationCreate', 'authorizations'),
    ('AUTHORIZATION_MANAGE', 'permission.authorizationManage', 'authorizations')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'AUTHORIZATION_VIEW', true),
    ('SUPER_ADMIN', 'AUTHORIZATION_CREATE', true),
    ('SUPER_ADMIN', 'AUTHORIZATION_MANAGE', true),
    ('COMMUNITY_ADMIN', 'AUTHORIZATION_VIEW', true),
    ('COMMUNITY_ADMIN', 'AUTHORIZATION_CREATE', true),
    ('COMMUNITY_ADMIN', 'AUTHORIZATION_MANAGE', true),
    ('PRESIDENT', 'AUTHORIZATION_VIEW', true),
    ('PRESIDENT', 'AUTHORIZATION_CREATE', true),
    ('PRESIDENT', 'AUTHORIZATION_MANAGE', true),
    ('SECRETARY', 'AUTHORIZATION_VIEW', true),
    ('SECRETARY', 'AUTHORIZATION_CREATE', true),
    ('SECRETARY', 'AUTHORIZATION_MANAGE', true),
    ('TREASURER', 'AUTHORIZATION_VIEW', true),
    ('TREASURER', 'AUTHORIZATION_CREATE', true),
    ('OWNER', 'AUTHORIZATION_VIEW', true),
    ('OWNER', 'AUTHORIZATION_CREATE', true),
    ('TENANT', 'AUTHORIZATION_VIEW', true),
    ('TENANT', 'AUTHORIZATION_CREATE', true),
    ('PROVIDER', 'AUTHORIZATION_VIEW', true),
    ('VIEWER', 'AUTHORIZATION_VIEW', true),
    ('VECINO', 'AUTHORIZATION_VIEW', true),
    ('VECINO', 'AUTHORIZATION_CREATE', true)
ON CONFLICT DO NOTHING;

CREATE TABLE community_authorization (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    grantor_membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    grantee_membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    scope VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    valid_from DATE NOT NULL,
    valid_until DATE,
    notes VARCHAR(1000),
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT authorization_scope_check CHECK (scope IN ('VOTE', 'WORK_PERMIT', 'INCIDENT', 'BOARD', 'LOST_FOUND')),
    CONSTRAINT authorization_status_check CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT authorization_not_self CHECK (grantor_membership_id <> grantee_membership_id),
    CONSTRAINT authorization_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX authorization_grantor_idx ON community_authorization (community_id, grantor_membership_id, created_at DESC);
CREATE INDEX authorization_grantee_idx ON community_authorization (community_id, grantee_membership_id, created_at DESC);
CREATE UNIQUE INDEX authorization_active_unique ON community_authorization
    (community_id, grantor_membership_id, grantee_membership_id, scope)
    WHERE status = 'ACTIVE';
