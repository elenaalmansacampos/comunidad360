CREATE TABLE fiscal_year (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    year INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    closed_at TIMESTAMPTZ,
    closed_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fiscal_year_unique UNIQUE (community_id, year),
    CONSTRAINT fiscal_year_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT fiscal_year_status_check CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT fiscal_year_dates_check CHECK (end_date >= start_date)
);

CREATE INDEX fiscal_year_community_idx ON fiscal_year (community_id, year DESC);

CREATE TABLE budget (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_income NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_expense NUMERIC(19,4) NOT NULL DEFAULT 0,
    notes VARCHAR(2000),
    approved_at TIMESTAMPTZ,
    approved_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT budget_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT budget_fiscal_year_fk
        FOREIGN KEY (fiscal_year_id, community_id) REFERENCES fiscal_year (id, community_id),
    CONSTRAINT budget_status_check CHECK (status IN ('DRAFT', 'APPROVED', 'CLOSED')),
    CONSTRAINT budget_amounts_check CHECK (total_income >= 0 AND total_expense >= 0)
);

CREATE INDEX budget_community_idx ON budget (community_id, fiscal_year_id);

CREATE TABLE budget_item (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    budget_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(300) NOT NULL,
    planned_amount NUMERIC(19,4) NOT NULL,
    position INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT budget_item_budget_fk
        FOREIGN KEY (budget_id, community_id) REFERENCES budget (id, community_id),
    CONSTRAINT budget_item_type_check CHECK (type IN ('INCOME', 'EXPENSE')),
    CONSTRAINT budget_item_amount_check CHECK (planned_amount >= 0)
);

CREATE INDEX budget_item_budget_idx ON budget_item (budget_id, position);

CREATE TABLE assessment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL,
    concept VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(19,4) NOT NULL,
    allocation_method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    issued_at TIMESTAMPTZ,
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT assessment_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT assessment_fiscal_year_fk
        FOREIGN KEY (fiscal_year_id, community_id) REFERENCES fiscal_year (id, community_id),
    CONSTRAINT assessment_type_check CHECK (type IN ('ORDINARY', 'EXTRAORDINARY')),
    CONSTRAINT assessment_method_check CHECK (allocation_method IN ('COEFFICIENT', 'EQUAL', 'CUSTOM')),
    CONSTRAINT assessment_status_check CHECK (status IN ('DRAFT', 'ISSUED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT assessment_amount_check CHECK (total_amount >= 0),
    CONSTRAINT assessment_period_check CHECK (period_end >= period_start)
);

CREATE INDEX assessment_community_idx ON assessment (community_id, fiscal_year_id, status);

CREATE TABLE assessment_charge (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL,
    assessment_id UUID NOT NULL,
    unit_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    coefficient_snapshot NUMERIC(9,6) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT charge_assessment_unique UNIQUE (assessment_id, unit_id),
    CONSTRAINT charge_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT charge_assessment_fk
        FOREIGN KEY (assessment_id, community_id) REFERENCES assessment (id, community_id),
    CONSTRAINT charge_unit_fk
        FOREIGN KEY (unit_id, community_id) REFERENCES unit (id, community_id),
    CONSTRAINT charge_amount_check CHECK (amount >= 0 AND paid_amount >= 0),
    CONSTRAINT charge_status_check CHECK (status IN ('PENDING', 'PARTIAL', 'PAID', 'CANCELLED'))
);

CREATE INDEX charge_community_idx ON assessment_charge (community_id, status);
CREATE INDEX charge_unit_idx ON assessment_charge (unit_id, status);

CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL,
    provider_name VARCHAR(200) NOT NULL,
    number VARCHAR(100) NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE,
    subtotal NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total NUMERIC(19,4) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT invoice_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT invoice_fiscal_year_fk
        FOREIGN KEY (fiscal_year_id, community_id) REFERENCES fiscal_year (id, community_id),
    CONSTRAINT invoice_status_check CHECK (status IN ('DRAFT', 'REGISTERED', 'PAID', 'CANCELLED')),
    CONSTRAINT invoice_amounts_check CHECK (subtotal >= 0 AND tax_amount >= 0 AND total >= 0 AND paid_amount >= 0),
    CONSTRAINT invoice_provider_number_unique UNIQUE (community_id, provider_name, number)
);

CREATE INDEX invoice_community_idx ON invoice (community_id, fiscal_year_id, status);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    unit_id UUID,
    assessment_charge_id UUID,
    invoice_id UUID,
    amount NUMERIC(19,4) NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(20) NOT NULL,
    reference VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    created_by_user_id UUID REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT payment_charge_fk
        FOREIGN KEY (assessment_charge_id, community_id) REFERENCES assessment_charge (id, community_id),
    CONSTRAINT payment_invoice_fk
        FOREIGN KEY (invoice_id, community_id) REFERENCES invoice (id, community_id),
    CONSTRAINT payment_unit_fk
        FOREIGN KEY (unit_id, community_id) REFERENCES unit (id, community_id),
    CONSTRAINT payment_amount_check CHECK (amount > 0),
    CONSTRAINT payment_target_check CHECK (assessment_charge_id IS NOT NULL OR invoice_id IS NOT NULL),
    CONSTRAINT payment_status_check CHECK (status IN ('REGISTERED', 'RECONCILED', 'CANCELLED'))
);

CREATE INDEX payment_community_idx ON payment (community_id, payment_date DESC);
CREATE INDEX payment_charge_idx ON payment (assessment_charge_id);

CREATE TABLE ledger_entry (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    fiscal_year_id UUID NOT NULL,
    entry_number BIGINT NOT NULL,
    entry_date DATE NOT NULL,
    type VARCHAR(20) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    reversal_of_id UUID,
    created_by_user_id UUID REFERENCES app_user (id),
    posted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ledger_number_unique UNIQUE (community_id, fiscal_year_id, entry_number),
    CONSTRAINT ledger_id_community_unique UNIQUE (id, community_id),
    CONSTRAINT ledger_fiscal_year_fk
        FOREIGN KEY (fiscal_year_id, community_id) REFERENCES fiscal_year (id, community_id),
    CONSTRAINT ledger_reversal_fk
        FOREIGN KEY (reversal_of_id, community_id) REFERENCES ledger_entry (id, community_id),
    CONSTRAINT ledger_type_check CHECK (type IN ('INCOME', 'EXPENSE', 'TRANSFER', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT ledger_status_check CHECK (status IN ('DRAFT', 'POSTED', 'VOID')),
    CONSTRAINT ledger_amount_check CHECK (amount >= 0)
);

CREATE INDEX ledger_community_idx ON ledger_entry (community_id, fiscal_year_id, entry_date DESC);

CREATE TABLE idempotency_record (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES community (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES app_user (id),
    operation VARCHAR(80) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    resource_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT idempotency_unique UNIQUE (community_id, user_id, operation, idempotency_key)
);

CREATE INDEX idempotency_created_idx ON idempotency_record (created_at);
