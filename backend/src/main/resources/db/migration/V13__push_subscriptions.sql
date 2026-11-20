CREATE TABLE push_subscription (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    endpoint VARCHAR(2048) NOT NULL,
    p256dh VARCHAR(255) NOT NULL,
    auth VARCHAR(255) NOT NULL,
    user_agent VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_success_at TIMESTAMPTZ,
    failure_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT push_subscription_endpoint_unique UNIQUE (endpoint)
);

CREATE INDEX push_subscription_user_idx ON push_subscription (user_id);
