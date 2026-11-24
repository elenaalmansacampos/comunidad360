CREATE TABLE stored_file (
    storage_key VARCHAR(500) PRIMARY KEY,
    content BYTEA NOT NULL,
    content_type VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
