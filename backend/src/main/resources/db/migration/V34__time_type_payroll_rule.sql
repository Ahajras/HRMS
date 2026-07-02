CREATE TABLE time_type_payroll_rule (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id           UUID         NOT NULL,
    time_type_id         UUID         NOT NULL REFERENCES time_type (id) ON DELETE CASCADE,
    payroll_component_id UUID         NOT NULL REFERENCES payroll_component (id) ON DELETE CASCADE,
    action               VARCHAR(20)  NOT NULL DEFAULT 'PAY',      -- PAY | DEDUCT | IGNORE
    percent              NUMERIC(7,2) NOT NULL DEFAULT 100.00,
    basis                VARCHAR(20)  NOT NULL DEFAULT 'HOURS',    -- HOURS | DAYS | FIXED
    affects_overtime     BOOLEAN      NOT NULL DEFAULT FALSE,
    process_separately   BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order           INTEGER      NOT NULL DEFAULT 100,
    remarks              VARCHAR(500),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by           VARCHAR(100),
    updated_at           TIMESTAMPTZ,
    updated_by           VARCHAR(100),
    version              BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_time_type_payroll_rule
    ON time_type_payroll_rule (time_type_id, payroll_component_id);

CREATE INDEX ix_time_type_payroll_rule_time_type
    ON time_type_payroll_rule (company_id, time_type_id, sort_order);
