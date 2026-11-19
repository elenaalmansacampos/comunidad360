CREATE TABLE building (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    address VARCHAR(300),
    city VARCHAR(100),
    postal_code VARCHAR(10),
    floors INTEGER,
    notes VARCHAR(2000),
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT building_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX building_community_idx ON building (community_id, archived_at);

CREATE TABLE unit (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    building_id UUID,
    code VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    floor VARCHAR(20),
    door VARCHAR(20),
    built_area NUMERIC(10,2),
    coefficient NUMERIC(9,6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT unit_type_check CHECK (type IN ('VIVIENDA', 'LOCAL', 'GARAJE', 'TRASTERO', 'ELEMENTO_COMUN')),
    CONSTRAINT unit_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT unit_coefficient_check CHECK (coefficient >= 0 AND coefficient <= 100),
    CONSTRAINT unit_community_code_unique UNIQUE (community_id, code),
    CONSTRAINT unit_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT unit_building_community_fk
        FOREIGN KEY (building_id, community_id) REFERENCES building (id, community_id)
);

CREATE INDEX unit_community_idx ON unit (community_id, archived_at);
CREATE INDEX unit_building_idx ON unit (building_id, archived_at);

CREATE TABLE common_element (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    building_id UUID,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(50),
    description VARCHAR(2000),
    archived_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT common_element_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT common_element_building_community_fk
        FOREIGN KEY (building_id, community_id) REFERENCES building (id, community_id)
);

CREATE INDEX common_element_community_idx ON common_element (community_id, archived_at);
