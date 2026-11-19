CREATE TABLE incident (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    building_id UUID,
    unit_id UUID,
    common_element_id UUID,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(10000) NOT NULL,
    category VARCHAR(100) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    reported_by_user_id UUID REFERENCES app_user (id),
    assigned_to_user_id UUID REFERENCES app_user (id),
    resolved_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT incident_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT incident_building_fk
        FOREIGN KEY (building_id, community_id) REFERENCES building (id, community_id),
    CONSTRAINT incident_unit_fk
        FOREIGN KEY (unit_id, community_id) REFERENCES unit (id, community_id),
    CONSTRAINT incident_common_element_fk
        FOREIGN KEY (common_element_id, community_id) REFERENCES common_element (id, community_id),
    CONSTRAINT incident_priority_check CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    CONSTRAINT incident_status_check CHECK (status IN ('OPEN', 'IN_PROGRESS', 'WAITING_PROVIDER', 'RESOLVED', 'CLOSED', 'CANCELLED'))
);

CREATE INDEX incident_community_idx ON incident (community_id, status, priority, created_at DESC);
CREATE INDEX incident_unit_idx ON incident (unit_id, created_at DESC);
CREATE INDEX incident_assigned_idx ON incident (community_id, assigned_to_user_id, status);

CREATE TABLE incident_comment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    author_user_id UUID REFERENCES app_user (id),
    body VARCHAR(5000) NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT incident_comment_incident_fk
        FOREIGN KEY (incident_id, community_id) REFERENCES incident (id, community_id)
);

CREATE INDEX incident_comment_incident_idx ON incident_comment (incident_id, created_at ASC);

CREATE TABLE incident_status_history (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id UUID REFERENCES app_user (id),
    changed_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(500),
    CONSTRAINT incident_history_incident_fk
        FOREIGN KEY (incident_id, community_id) REFERENCES incident (id, community_id)
);

CREATE INDEX incident_history_incident_idx ON incident_status_history (incident_id, changed_at ASC);

CREATE TABLE incident_attachment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    incident_id UUID NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT incident_attachment_incident_fk
        FOREIGN KEY (incident_id, community_id) REFERENCES incident (id, community_id),
    CONSTRAINT incident_attachment_size_check CHECK (size_bytes > 0)
);

CREATE INDEX incident_attachment_incident_idx ON incident_attachment (incident_id, created_at ASC);
