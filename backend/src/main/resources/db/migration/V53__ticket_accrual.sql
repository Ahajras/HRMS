ALTER TABLE employee ADD COLUMN IF NOT EXISTS home_airport_code VARCHAR(20);
ALTER TABLE employee ADD COLUMN IF NOT EXISTS work_airport_code VARCHAR(20);

CREATE TABLE ticket_fare (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    from_airport_code VARCHAR(20) NOT NULL,
    to_airport_code VARCHAR(20) NOT NULL,
    amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    currency_code VARCHAR(3),
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_ticket_fare_lookup ON ticket_fare(company_id, from_airport_code, to_airport_code, status, effective_from);

CREATE TABLE ticket_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    leave_request_id UUID,
    entry_type VARCHAR(30) NOT NULL,
    entry_date DATE NOT NULL,
    amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    from_airport_code VARCHAR(20),
    to_airport_code VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    remarks VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_ticket_ledger_employee ON ticket_ledger(company_id, employee_id, entry_date);
CREATE UNIQUE INDEX ux_ticket_ledger_leave_active ON ticket_ledger(company_id, leave_request_id)
    WHERE leave_request_id IS NOT NULL AND status = 'ACTIVE';
