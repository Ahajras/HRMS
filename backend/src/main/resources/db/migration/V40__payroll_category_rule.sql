CREATE TABLE payroll_category_rule (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL,
    payroll_rule_id UUID         NOT NULL REFERENCES payroll_rule (id) ON DELETE CASCADE,
    category        VARCHAR(30)  NOT NULL,
    basis           VARCHAR(30)  NOT NULL DEFAULT 'ACTUAL_PAYABLE',
    divisor_mode    VARCHAR(20)  NOT NULL DEFAULT 'INHERIT',
    month_divisor   NUMERIC(6,2),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_payroll_category_rule
    ON payroll_category_rule (payroll_rule_id, category)
    WHERE status = 'ACTIVE';

INSERT INTO payroll_category_rule (company_id, payroll_rule_id, category, basis, divisor_mode, month_divisor)
SELECT company_id, id, 'SALARY', 'FULL_MONTH', 'INHERIT', NULL
FROM payroll_rule
ON CONFLICT DO NOTHING;

INSERT INTO payroll_category_rule (company_id, payroll_rule_id, category, basis, divisor_mode, month_divisor)
SELECT company_id, id, 'ALLOWANCE', 'ACTUAL_PAYABLE', 'INHERIT', NULL
FROM payroll_rule
ON CONFLICT DO NOTHING;
