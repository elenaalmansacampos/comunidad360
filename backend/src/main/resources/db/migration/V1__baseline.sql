CREATE TABLE app_user (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'es',
    status VARCHAR(30) NOT NULL,
    email_verified_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT app_user_status_check CHECK (status IN ('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DISABLED'))
);

CREATE TABLE community (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    tax_id VARCHAR(20),
    address VARCHAR(300),
    city VARCHAR(100),
    province VARCHAR(100),
    postal_code VARCHAR(10),
    timezone VARCHAR(50) NOT NULL DEFAULT 'Europe/Madrid',
    default_language VARCHAR(10) NOT NULL DEFAULT 'es',
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT community_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'ARCHIVED'))
);

CREATE TABLE role (
    code VARCHAR(40) PRIMARY KEY,
    name_key VARCHAR(120) NOT NULL,
    scope VARCHAR(20) NOT NULL,
    is_system BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT role_scope_check CHECK (scope IN ('PLATFORM', 'COMMUNITY', 'PROPERTY', 'PROVIDER'))
);

CREATE TABLE permission (
    code VARCHAR(60) PRIMARY KEY,
    description_key VARCHAR(120) NOT NULL,
    module VARCHAR(40) NOT NULL
);

CREATE TABLE role_permission (
    role_code VARCHAR(40) NOT NULL REFERENCES role (code) ON DELETE CASCADE,
    permission_code VARCHAR(60) NOT NULL REFERENCES permission (code) ON DELETE CASCADE,
    granted BOOLEAN NOT NULL DEFAULT true,
    PRIMARY KEY (role_code, permission_code)
);

CREATE TABLE membership (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'INVITED',
    joined_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT membership_status_check CHECK (status IN ('INVITED', 'ACTIVE', 'SUSPENDED', 'REMOVED')),
    CONSTRAINT membership_community_user_unique UNIQUE (community_id, user_id),
    CONSTRAINT membership_id_community_unique UNIQUE (id, community_id)
);

CREATE TABLE membership_role (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    role_code VARCHAR(40) NOT NULL REFERENCES role (code),
    assigned_at TIMESTAMPTZ NOT NULL,
    assigned_by_user_id UUID REFERENCES app_user (id),
    CONSTRAINT membership_role_unique UNIQUE (membership_id, role_code)
);

CREATE TABLE membership_permission (
    membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    permission_code VARCHAR(60) NOT NULL REFERENCES permission (code),
    granted BOOLEAN NOT NULL,
    granted_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (membership_id, permission_code)
);

CREATE TABLE invitation (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    email VARCHAR(320) NOT NULL,
    role_code VARCHAR(40) NOT NULL REFERENCES role (code),
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    invited_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX invitation_community_idx ON invitation (community_id, created_at DESC);
CREATE INDEX invitation_email_idx ON invitation (email, created_at DESC);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID REFERENCES refresh_token (id),
    user_agent VARCHAR(500),
    ip_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX refresh_token_user_idx ON refresh_token (user_id, created_at DESC);

CREATE TABLE password_reset_token (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    community_id UUID,
    actor_user_id UUID,
    actor_type VARCHAR(20) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60) NOT NULL,
    entity_id UUID,
    before_data JSONB,
    after_data JSONB,
    ip_hash VARCHAR(64),
    user_agent VARCHAR(500),
    trace_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT audit_actor_type_check CHECK (actor_type IN ('USER', 'SUPER_ADMIN', 'SYSTEM'))
);

CREATE INDEX audit_log_community_created_idx ON audit_log (community_id, created_at DESC);
CREATE INDEX audit_log_entity_idx ON audit_log (entity_type, entity_id);
