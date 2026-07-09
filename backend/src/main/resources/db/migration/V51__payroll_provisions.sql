CREATE TABLE provision_run (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    period_id UUID NOT NULL,
    project_id UUID,
    pay_group VARCHAR(30) NOT NULL DEFAULT 'ALL',
    provision_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CALCULATED',
    calculated_at TIMESTAMPTZ,
    employee_count INTEGER NOT NULL DEFAULT 0,
    total_eligible_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_provision_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_provision_run_company_period ON provision_run(company_id, period_id);
CREATE INDEX ix_provision_run_scope ON provision_run(company_id, period_id, project_id, pay_group, provision_type);

CREATE TABLE provision_result (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    run_id UUID NOT NULL REFERENCES provision_run(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL,
    employee_number VARCHAR(50),
    employee_name VARCHAR(250),
    project_id UUID,
    pay_group VARCHAR(30),
    eligible_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    provision_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    formula_note VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'OK',
    message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_provision_result_run ON provision_result(run_id, employee_number);
CREATE INDEX ix_provision_result_employee ON provision_result(company_id, employee_id);
