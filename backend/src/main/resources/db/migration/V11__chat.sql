CREATE TABLE chat_channel (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    name VARCHAR(200),
    description VARCHAR(2000),
    created_by_user_id UUID REFERENCES app_user (id),
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_channel_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT chat_channel_type_check CHECK (type IN ('GENERAL', 'THEMATIC', 'PRIVATE'))
);

CREATE UNIQUE INDEX chat_channel_general_unique
    ON chat_channel (community_id)
    WHERE type = 'GENERAL' AND archived_at IS NULL;

CREATE TABLE chat_channel_member (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    membership_id UUID NOT NULL REFERENCES membership (id),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chat_channel_member_channel_fk
        FOREIGN KEY (channel_id, community_id) REFERENCES chat_channel (id, community_id),
    CONSTRAINT chat_channel_member_unique UNIQUE (channel_id, membership_id),
    CONSTRAINT chat_channel_member_role_check CHECK (role IN ('MEMBER', 'MODERATOR'))
);

CREATE TABLE chat_message (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    channel_id UUID NOT NULL,
    sender_user_id UUID REFERENCES app_user (id),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    deleted_by_user_id UUID REFERENCES app_user (id),
    CONSTRAINT chat_message_channel_fk
        FOREIGN KEY (channel_id, community_id) REFERENCES chat_channel (id, community_id),
    CONSTRAINT chat_message_body_check CHECK (length(trim(body)) > 0)
);

CREATE INDEX chat_message_channel_idx ON chat_message (channel_id, created_at DESC);
