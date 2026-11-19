CREATE TABLE meeting (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    title VARCHAR(300) NOT NULL,
    type VARCHAR(20) NOT NULL,
    scheduled_start TIMESTAMPTZ NOT NULL,
    scheduled_end TIMESTAMPTZ,
    location VARCHAR(300),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    convocation_document_id UUID REFERENCES document (id),
    minutes_document_id UUID REFERENCES document (id),
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT meeting_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT meeting_type_check CHECK (type IN ('ORDINARY', 'EXTRAORDINARY')),
    CONSTRAINT meeting_status_check CHECK (status IN ('DRAFT', 'CONVENED', 'HELD', 'CANCELLED')),
    CONSTRAINT meeting_dates_check CHECK (scheduled_end IS NULL OR scheduled_end >= scheduled_start)
);

CREATE INDEX meeting_community_start_idx ON meeting (community_id, scheduled_start DESC);

CREATE TABLE meeting_agenda_item (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    meeting_id UUID NOT NULL,
    position INTEGER NOT NULL,
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT meeting_agenda_meeting_fk
        FOREIGN KEY (meeting_id, community_id) REFERENCES meeting (id, community_id),
    CONSTRAINT meeting_agenda_position_unique UNIQUE (meeting_id, position)
);

CREATE INDEX meeting_agenda_meeting_idx ON meeting_agenda_item (meeting_id, position);

CREATE TABLE meeting_attendance (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    meeting_id UUID NOT NULL,
    membership_id UUID NOT NULL REFERENCES membership (id),
    attendance_type VARCHAR(20) NOT NULL,
    represented_by_membership_id UUID REFERENCES membership (id),
    coefficient_snapshot NUMERIC(9,6) NOT NULL DEFAULT 0,
    recorded_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT meeting_attendance_meeting_fk
        FOREIGN KEY (meeting_id, community_id) REFERENCES meeting (id, community_id),
    CONSTRAINT meeting_attendance_unique UNIQUE (meeting_id, membership_id),
    CONSTRAINT meeting_attendance_type_check
        CHECK (attendance_type IN ('IN_PERSON', 'ONLINE', 'REPRESENTED', 'ABSENT')),
    CONSTRAINT meeting_attendance_representative_check
        CHECK (represented_by_membership_id IS NULL OR represented_by_membership_id <> membership_id)
);

CREATE INDEX meeting_attendance_meeting_idx ON meeting_attendance (meeting_id, attendance_type);

CREATE TABLE meeting_agreement (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    meeting_id UUID NOT NULL,
    agenda_item_id UUID REFERENCES meeting_agenda_item (id),
    title VARCHAR(300) NOT NULL,
    description VARCHAR(2000),
    votes_for INTEGER NOT NULL DEFAULT 0,
    votes_against INTEGER NOT NULL DEFAULT 0,
    abstentions INTEGER NOT NULL DEFAULT 0,
    coefficient_for NUMERIC(9,6) NOT NULL DEFAULT 0,
    coefficient_against NUMERIC(9,6) NOT NULL DEFAULT 0,
    coefficient_abstention NUMERIC(9,6) NOT NULL DEFAULT 0,
    adopted BOOLEAN,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT meeting_agreement_meeting_fk
        FOREIGN KEY (meeting_id, community_id) REFERENCES meeting (id, community_id),
    CONSTRAINT meeting_agreement_votes_check
        CHECK (votes_for >= 0 AND votes_against >= 0 AND abstentions >= 0),
    CONSTRAINT meeting_agreement_coefficients_check
        CHECK (coefficient_for >= 0 AND coefficient_against >= 0 AND coefficient_abstention >= 0)
);

CREATE INDEX meeting_agreement_meeting_idx ON meeting_agreement (meeting_id, created_at);

CREATE TABLE notification (
    id UUID PRIMARY KEY,
    community_id UUID REFERENCES community (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    type VARCHAR(60) NOT NULL,
    category VARCHAR(60) NOT NULL,
    template_key VARCHAR(120) NOT NULL,
    params JSONB NOT NULL DEFAULT '{}'::jsonb,
    locale VARCHAR(10) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    error_code VARCHAR(80),
    CONSTRAINT notification_channel_check CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    CONSTRAINT notification_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'READ'))
);

CREATE INDEX notification_user_idx ON notification (user_id, created_at DESC);
CREATE INDEX notification_community_idx ON notification (community_id, created_at DESC);
CREATE INDEX notification_unread_idx ON notification (user_id, created_at DESC) WHERE status <> 'READ';

CREATE TABLE notification_preference (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    community_id UUID REFERENCES community (id) ON DELETE CASCADE,
    category VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT notification_preference_channel_check CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    CONSTRAINT notification_preference_unique
        UNIQUE NULLS NOT DISTINCT (user_id, community_id, category, channel)
);

CREATE TABLE notification_delivery (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notification (id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    attempt INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(200),
    error_code VARCHAR(80),
    attempted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT notification_delivery_status_check CHECK (status IN ('SENT', 'FAILED', 'RETRYING'))
);

CREATE INDEX notification_delivery_notification_idx
    ON notification_delivery (notification_id, attempted_at DESC);
