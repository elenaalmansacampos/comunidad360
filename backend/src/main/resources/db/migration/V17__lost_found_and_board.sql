CREATE TABLE lost_item (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    type VARCHAR(20) NOT NULL,
    location VARCHAR(200),
    event_date DATE,
    contact VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT lost_item_type_check CHECK (type IN ('LOST', 'FOUND')),
    CONSTRAINT lost_item_status_check CHECK (status IN ('OPEN', 'RESOLVED', 'ARCHIVED')),
    CONSTRAINT lost_item_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX lost_item_community_idx ON lost_item (community_id, created_at DESC);

CREATE TABLE board_note (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    color VARCHAR(20) NOT NULL DEFAULT 'YELLOW',
    pinned BOOLEAN NOT NULL DEFAULT false,
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT board_note_color_check CHECK (color IN ('YELLOW', 'PINK', 'BLUE', 'GREEN', 'ORANGE')),
    CONSTRAINT board_note_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX board_note_community_idx ON board_note (community_id, pinned DESC, created_at DESC);

INSERT INTO permission (code, description_key, module) VALUES
    ('LOST_ITEM_VIEW', 'permission.lostItemView', 'lostfound'),
    ('LOST_ITEM_MANAGE', 'permission.lostItemManage', 'lostfound'),
    ('BOARD_VIEW', 'permission.boardView', 'board'),
    ('BOARD_CREATE', 'permission.boardCreate', 'board'),
    ('BOARD_MANAGE', 'permission.boardManage', 'board')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'LOST_ITEM_VIEW', true),
    ('SUPER_ADMIN', 'LOST_ITEM_MANAGE', true),
    ('SUPER_ADMIN', 'BOARD_VIEW', true),
    ('SUPER_ADMIN', 'BOARD_CREATE', true),
    ('SUPER_ADMIN', 'BOARD_MANAGE', true),

    ('COMMUNITY_ADMIN', 'LOST_ITEM_VIEW', true),
    ('COMMUNITY_ADMIN', 'LOST_ITEM_MANAGE', true),
    ('COMMUNITY_ADMIN', 'BOARD_VIEW', true),
    ('COMMUNITY_ADMIN', 'BOARD_CREATE', true),
    ('COMMUNITY_ADMIN', 'BOARD_MANAGE', true),

    ('PRESIDENT', 'LOST_ITEM_VIEW', true),
    ('PRESIDENT', 'LOST_ITEM_MANAGE', true),
    ('PRESIDENT', 'BOARD_VIEW', true),
    ('PRESIDENT', 'BOARD_CREATE', true),
    ('PRESIDENT', 'BOARD_MANAGE', true),

    ('SECRETARY', 'LOST_ITEM_VIEW', true),
    ('SECRETARY', 'LOST_ITEM_MANAGE', true),
    ('SECRETARY', 'BOARD_VIEW', true),
    ('SECRETARY', 'BOARD_CREATE', true),
    ('SECRETARY', 'BOARD_MANAGE', true),

    ('TREASURER', 'LOST_ITEM_VIEW', true),
    ('TREASURER', 'BOARD_VIEW', true),
    ('TREASURER', 'BOARD_CREATE', true),

    ('OWNER', 'LOST_ITEM_VIEW', true),
    ('OWNER', 'BOARD_VIEW', true),
    ('OWNER', 'BOARD_CREATE', true),

    ('TENANT', 'LOST_ITEM_VIEW', true),
    ('TENANT', 'BOARD_VIEW', true),
    ('TENANT', 'BOARD_CREATE', true),

    ('PROVIDER', 'LOST_ITEM_VIEW', true),
    ('PROVIDER', 'BOARD_VIEW', true),
    ('PROVIDER', 'BOARD_CREATE', true)
ON CONFLICT DO NOTHING;
