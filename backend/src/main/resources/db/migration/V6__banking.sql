ALTER TABLE payment
    ADD CONSTRAINT payment_id_community_unique UNIQUE (id, community_id);

CREATE TABLE bank_account (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    bank_name VARCHAR(200),
    iban_last4 VARCHAR(4),
    iban_masked VARCHAR(50),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    source VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT bank_account_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT bank_account_status_check CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT bank_account_source_check CHECK (source IN ('MANUAL', 'CSV', 'PSD2'))
);

CREATE INDEX bank_account_community_idx ON bank_account (community_id, status);

CREATE TABLE bank_import_batch (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    bank_account_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_hash VARCHAR(64) NOT NULL,
    imported_by_user_id UUID REFERENCES app_user (id),
    imported_at TIMESTAMPTZ NOT NULL,
    row_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    duplicate_count INTEGER NOT NULL DEFAULT 0,
    error_count INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT bank_import_account_fk
        FOREIGN KEY (bank_account_id, community_id) REFERENCES bank_account (id, community_id),
    CONSTRAINT bank_import_status_check CHECK (status IN ('PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX bank_import_community_idx ON bank_import_batch (community_id, imported_at DESC);

CREATE TABLE bank_transaction (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    bank_account_id UUID NOT NULL,
    import_batch_id UUID REFERENCES bank_import_batch (id),
    external_id VARCHAR(200),
    value_date DATE NOT NULL,
    booking_date DATE,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    balance_after NUMERIC(19,4),
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    raw_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNRECONCILED',
    imported_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT bank_transaction_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT bank_transaction_account_fk
        FOREIGN KEY (bank_account_id, community_id) REFERENCES bank_account (id, community_id),
    CONSTRAINT bank_transaction_import_fk FOREIGN KEY (import_batch_id) REFERENCES bank_import_batch (id),
    CONSTRAINT bank_transaction_status_check CHECK (status IN ('UNRECONCILED', 'RECONCILED', 'IGNORED')),
    CONSTRAINT bank_transaction_unique UNIQUE (bank_account_id, raw_hash)
);

CREATE INDEX bank_transaction_community_idx ON bank_transaction (community_id, status, value_date DESC);
CREATE INDEX bank_transaction_account_idx ON bank_transaction (bank_account_id, value_date DESC);

CREATE TABLE reconciliation (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    bank_transaction_id UUID NOT NULL,
    payment_id UUID,
    ledger_entry_id UUID,
    matched_amount NUMERIC(19,4) NOT NULL,
    matched_by_user_id UUID REFERENCES app_user (id),
    matched_at TIMESTAMPTZ NOT NULL,
    notes VARCHAR(500),
    CONSTRAINT reconciliation_transaction_fk
        FOREIGN KEY (bank_transaction_id, community_id) REFERENCES bank_transaction (id, community_id),
    CONSTRAINT reconciliation_payment_fk
        FOREIGN KEY (payment_id, community_id) REFERENCES payment (id, community_id),
    CONSTRAINT reconciliation_ledger_fk
        FOREIGN KEY (ledger_entry_id, community_id) REFERENCES ledger_entry (id, community_id),
    CONSTRAINT reconciliation_target_check
        CHECK ((payment_id IS NOT NULL AND ledger_entry_id IS NULL)
            OR (payment_id IS NULL AND ledger_entry_id IS NOT NULL)),
    CONSTRAINT reconciliation_transaction_unique UNIQUE (bank_transaction_id),
    CONSTRAINT reconciliation_amount_check CHECK (matched_amount > 0)
);

CREATE UNIQUE INDEX reconciliation_payment_unique
    ON reconciliation (payment_id) WHERE payment_id IS NOT NULL;
CREATE UNIQUE INDEX reconciliation_ledger_unique
    ON reconciliation (ledger_entry_id) WHERE ledger_entry_id IS NOT NULL;
CREATE INDEX reconciliation_community_idx ON reconciliation (community_id, matched_at DESC);
