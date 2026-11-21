CREATE TABLE report_schedule (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    recipients VARCHAR(1000) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ NOT NULL,
    last_status VARCHAR(20),
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT report_schedule_frequency_check
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY'))
);

CREATE INDEX report_schedule_due_idx ON report_schedule (enabled, next_run_at);
CREATE INDEX report_schedule_community_idx ON report_schedule (community_id);
