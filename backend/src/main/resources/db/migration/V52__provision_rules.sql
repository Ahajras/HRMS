CREATE TABLE provision_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    project_id UUID,
    pay_group VARCHAR(30) NOT NULL DEFAULT 'ALL',
    provision_type VARCHAR(30) NOT NULL,
    name VARCHAR(150) NOT NULL,
    basis_mode VARCHAR(30) NOT NULL DEFAULT 'COMPONENT_FLAGS',
    basis_categories VARCHAR(500),
    basis_component_codes VARCHAR(1000),
    formula_expression VARCHAR(1000) NOT NULL,
    divisor NUMERIC(18,4) NOT NULL DEFAULT 365,
    fixed_amount NUMERIC(18,4) NOT NULL DEFAULT 0,
    entitlement_days_under_five NUMERIC(18,4) NOT NULL DEFAULT 21,
    entitlement_days_five_or_more NUMERIC(18,4) NOT NULL DEFAULT 28,
    ticket_cycle_months INTEGER NOT NULL DEFAULT 12,
    effective_from DATE NOT NULL,
    effective_to DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by VARCHAR(100),
    updated_at TIMESTAMPTZ,
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX ix_provision_rule_scope ON provision_rule(company_id, provision_type, project_id, pay_group, status);
