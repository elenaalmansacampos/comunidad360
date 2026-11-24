INSERT INTO permission (code, description_key, module) VALUES
    ('DIRECTORY_VIEW', 'permission.directoryView', 'directory'),
    ('DIRECTORY_MANAGE', 'permission.directoryManage', 'directory')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'DIRECTORY_VIEW', true),
    ('SUPER_ADMIN', 'DIRECTORY_MANAGE', true),
    ('COMMUNITY_ADMIN', 'DIRECTORY_VIEW', true),
    ('COMMUNITY_ADMIN', 'DIRECTORY_MANAGE', true),
    ('PRESIDENT', 'DIRECTORY_VIEW', true),
    ('PRESIDENT', 'DIRECTORY_MANAGE', true),
    ('SECRETARY', 'DIRECTORY_VIEW', true),
    ('SECRETARY', 'DIRECTORY_MANAGE', true),
    ('TREASURER', 'DIRECTORY_VIEW', true),
    ('OWNER', 'DIRECTORY_VIEW', true),
    ('TENANT', 'DIRECTORY_VIEW', true),
    ('PROVIDER', 'DIRECTORY_VIEW', true),
    ('VIEWER', 'DIRECTORY_VIEW', true),
    ('VECINO', 'DIRECTORY_VIEW', true)
ON CONFLICT DO NOTHING;

CREATE TABLE neighbor_profile (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    membership_id UUID NOT NULL UNIQUE REFERENCES membership (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id),
    phone VARCHAR(40),
    show_email BOOLEAN NOT NULL DEFAULT true,
    show_phone BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT neighbor_profile_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX neighbor_profile_community_idx ON neighbor_profile (community_id);
