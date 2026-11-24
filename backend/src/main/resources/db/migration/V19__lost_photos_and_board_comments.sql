ALTER TABLE lost_item
    ADD COLUMN photo_key VARCHAR(500),
    ADD COLUMN photo_content_type VARCHAR(100),
    ADD COLUMN photo_file_name VARCHAR(255);

CREATE TABLE board_note_comment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    note_id UUID NOT NULL,
    author_user_id UUID REFERENCES app_user (id),
    body VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT board_note_comment_note_fk
        FOREIGN KEY (note_id, community_id) REFERENCES board_note (id, community_id) ON DELETE CASCADE,
    CONSTRAINT board_note_comment_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX board_note_comment_note_idx ON board_note_comment (note_id, created_at);
