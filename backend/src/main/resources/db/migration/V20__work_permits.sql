CREATE TABLE work_permit (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    location VARCHAR(200),
    start_date DATE,
    end_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    created_by_user_id UUID REFERENCES app_user (id),
    reviewed_by_user_id UUID REFERENCES app_user (id),
    reviewed_at TIMESTAMPTZ,
    review_notes VARCHAR(1000),
    file_key VARCHAR(500),
    file_content_type VARCHAR(100),
    file_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT work_permit_status_check
        CHECK (status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT work_permit_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX work_permit_community_idx ON work_permit (community_id, created_at DESC);

INSERT INTO permission (code, description_key, module) VALUES
    ('WORK_PERMIT_VIEW', 'permission.workPermitView', 'works'),
    ('WORK_PERMIT_CREATE', 'permission.workPermitCreate', 'works'),
    ('WORK_PERMIT_MANAGE', 'permission.workPermitManage', 'works')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'WORK_PERMIT_VIEW', true),
    ('SUPER_ADMIN', 'WORK_PERMIT_CREATE', true),
    ('SUPER_ADMIN', 'WORK_PERMIT_MANAGE', true),

    ('COMMUNITY_ADMIN', 'WORK_PERMIT_VIEW', true),
    ('COMMUNITY_ADMIN', 'WORK_PERMIT_CREATE', true),
    ('COMMUNITY_ADMIN', 'WORK_PERMIT_MANAGE', true),

    ('PRESIDENT', 'WORK_PERMIT_VIEW', true),
    ('PRESIDENT', 'WORK_PERMIT_CREATE', true),
    ('PRESIDENT', 'WORK_PERMIT_MANAGE', true),

    ('SECRETARY', 'WORK_PERMIT_VIEW', true),
    ('SECRETARY', 'WORK_PERMIT_CREATE', true),
    ('SECRETARY', 'WORK_PERMIT_MANAGE', true),

    ('TREASURER', 'WORK_PERMIT_VIEW', true),
    ('TREASURER', 'WORK_PERMIT_CREATE', true),

    ('OWNER', 'WORK_PERMIT_VIEW', true),
    ('OWNER', 'WORK_PERMIT_CREATE', true),

    ('TENANT', 'WORK_PERMIT_VIEW', true),
    ('TENANT', 'WORK_PERMIT_CREATE', true),

    ('PROVIDER', 'WORK_PERMIT_VIEW', true),

    ('VIEWER', 'WORK_PERMIT_VIEW', true)
ON CONFLICT DO NOTHING;
