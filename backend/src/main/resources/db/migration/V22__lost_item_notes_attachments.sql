CREATE TABLE lost_item_note (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    item_id UUID NOT NULL,
    author_user_id UUID REFERENCES app_user (id),
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT lost_item_note_item_fk
        FOREIGN KEY (item_id, community_id) REFERENCES lost_item (id, community_id) ON DELETE CASCADE,
    CONSTRAINT lost_item_note_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX lost_item_note_item_idx ON lost_item_note (item_id, created_at);

CREATE TABLE lost_item_attachment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    item_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT lost_item_attachment_item_fk
        FOREIGN KEY (item_id, community_id) REFERENCES lost_item (id, community_id) ON DELETE CASCADE,
    CONSTRAINT lost_item_attachment_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX lost_item_attachment_item_idx ON lost_item_attachment (item_id, created_at);
