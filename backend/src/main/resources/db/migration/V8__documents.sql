CREATE TABLE document (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    visibility VARCHAR(20) NOT NULL,
    owner_user_id UUID REFERENCES app_user (id),
    current_version_id UUID,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT document_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT document_category_check CHECK (category IN ('ACTA', 'ESTATUTO', 'CONTRATO', 'FACTURA', 'PRESUPUESTO', 'INFORME', 'CERTIFICADO', 'OTRO')),
    CONSTRAINT document_visibility_check CHECK (visibility IN ('COMMUNITY', 'BOARD', 'OWNERS', 'ROLE', 'PRIVATE')),
    CONSTRAINT document_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX document_community_idx ON document (community_id, category, status, created_at DESC);

CREATE TABLE document_version (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    document_id UUID NOT NULL,
    version_number INTEGER NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(150) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    uploaded_by_user_id UUID REFERENCES app_user (id),
    uploaded_at TIMESTAMPTZ NOT NULL,
    scan_status VARCHAR(20) NOT NULL DEFAULT 'CLEAN',
    CONSTRAINT document_version_document_fk
        FOREIGN KEY (document_id, community_id) REFERENCES document (id, community_id),
    CONSTRAINT document_version_unique UNIQUE (document_id, version_number),
    CONSTRAINT document_version_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT document_version_size_check CHECK (size_bytes > 0),
    CONSTRAINT document_version_scan_check CHECK (scan_status IN ('PENDING', 'CLEAN', 'INFECTED', 'ERROR'))
);

CREATE INDEX document_version_document_idx ON document_version (document_id, version_number DESC);

ALTER TABLE document
    ADD CONSTRAINT document_current_version_fk
        FOREIGN KEY (current_version_id, community_id) REFERENCES document_version (id, community_id);

CREATE TABLE document_acl (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    document_id UUID NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    principal_id VARCHAR(100) NOT NULL,
    permission VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT document_acl_document_fk
        FOREIGN KEY (document_id, community_id) REFERENCES document (id, community_id),
    CONSTRAINT document_acl_principal_check CHECK (principal_type IN ('USER', 'ROLE', 'MEMBERSHIP')),
    CONSTRAINT document_acl_permission_check CHECK (permission IN ('VIEW', 'DOWNLOAD', 'MANAGE')),
    CONSTRAINT document_acl_unique UNIQUE (document_id, principal_type, principal_id, permission)
);

CREATE INDEX document_acl_document_idx ON document_acl (document_id);

CREATE TABLE document_access_log (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    document_id UUID NOT NULL,
    document_version_id UUID,
    user_id UUID REFERENCES app_user (id),
    action VARCHAR(20) NOT NULL,
    ip_hash VARCHAR(64),
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT document_access_document_fk
        FOREIGN KEY (document_id, community_id) REFERENCES document (id, community_id),
    CONSTRAINT document_access_action_check CHECK (action IN ('VIEW', 'DOWNLOAD', 'UPLOAD', 'UPDATE', 'DELETE'))
);

CREATE INDEX document_access_document_idx ON document_access_log (document_id, created_at DESC);
CREATE INDEX document_access_community_idx ON document_access_log (community_id, created_at DESC);
