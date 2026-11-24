INSERT INTO permission (code, description_key, module) VALUES
    ('RESERVATION_VIEW', 'permission.reservationView', 'reservations'),
    ('RESERVATION_CREATE', 'permission.reservationCreate', 'reservations'),
    ('RESERVATION_MANAGE', 'permission.reservationManage', 'reservations'),
    ('AREA_MANAGE', 'permission.areaManage', 'reservations')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permission (role_code, permission_code, granted) VALUES
    ('SUPER_ADMIN', 'RESERVATION_VIEW', true),
    ('SUPER_ADMIN', 'RESERVATION_CREATE', true),
    ('SUPER_ADMIN', 'RESERVATION_MANAGE', true),
    ('SUPER_ADMIN', 'AREA_MANAGE', true),
    ('COMMUNITY_ADMIN', 'RESERVATION_VIEW', true),
    ('COMMUNITY_ADMIN', 'RESERVATION_CREATE', true),
    ('COMMUNITY_ADMIN', 'RESERVATION_MANAGE', true),
    ('COMMUNITY_ADMIN', 'AREA_MANAGE', true),
    ('PRESIDENT', 'RESERVATION_VIEW', true),
    ('PRESIDENT', 'RESERVATION_CREATE', true),
    ('PRESIDENT', 'RESERVATION_MANAGE', true),
    ('PRESIDENT', 'AREA_MANAGE', true),
    ('SECRETARY', 'RESERVATION_VIEW', true),
    ('SECRETARY', 'RESERVATION_CREATE', true),
    ('SECRETARY', 'RESERVATION_MANAGE', true),
    ('SECRETARY', 'AREA_MANAGE', true),
    ('TREASURER', 'RESERVATION_VIEW', true),
    ('TREASURER', 'RESERVATION_CREATE', true),
    ('OWNER', 'RESERVATION_VIEW', true),
    ('OWNER', 'RESERVATION_CREATE', true),
    ('TENANT', 'RESERVATION_VIEW', true),
    ('TENANT', 'RESERVATION_CREATE', true),
    ('PROVIDER', 'RESERVATION_VIEW', true),
    ('VIEWER', 'RESERVATION_VIEW', true),
    ('VECINO', 'RESERVATION_VIEW', true),
    ('VECINO', 'RESERVATION_CREATE', true)
ON CONFLICT DO NOTHING;

CREATE TABLE common_area (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(2000),
    location VARCHAR(200),
    capacity INTEGER,
    requires_approval BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT common_area_id_community_unique UNIQUE (id, community_id)
);

CREATE INDEX common_area_community_idx ON common_area (community_id, active, name);

CREATE TABLE area_reservation (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    area_id UUID NOT NULL,
    membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id),
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    status VARCHAR(20) NOT NULL,
    notes VARCHAR(1000),
    reviewed_by_user_id UUID REFERENCES app_user (id),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT area_reservation_area_fk
        FOREIGN KEY (area_id, community_id) REFERENCES common_area (id, community_id) ON DELETE CASCADE,
    CONSTRAINT area_reservation_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT area_reservation_status_check
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT area_reservation_time_check CHECK (start_time < end_time)
);

CREATE INDEX area_reservation_community_idx
    ON area_reservation (community_id, reservation_date, start_time);
CREATE INDEX area_reservation_area_idx
    ON area_reservation (area_id, reservation_date, start_time);
CREATE INDEX area_reservation_membership_idx
    ON area_reservation (community_id, membership_id, reservation_date DESC);
