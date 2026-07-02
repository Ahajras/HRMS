CREATE TABLE payroll_rule (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id               UUID         NOT NULL,
    pay_group                VARCHAR(20)  NOT NULL,
    pay_item_basis           VARCHAR(30)  NOT NULL DEFAULT 'FIXED_AMOUNT',
    ot_multiplier            NUMERIC(8,4) NOT NULL DEFAULT 1.2500,
    standard_hours_per_day   NUMERIC(5,2) NOT NULL DEFAULT 8.00,
    status                   VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by               VARCHAR(100),
    updated_at               TIMESTAMPTZ,
    updated_by               VARCHAR(100),
    version                  BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uq_payroll_rule_group
    ON payroll_rule (company_id, pay_group)
    WHERE status = 'ACTIVE';

INSERT INTO payroll_rule (company_id, pay_group, pay_item_basis, ot_multiplier, standard_hours_per_day)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'MONTHLY', 'FIXED_AMOUNT', 1.2500, 8.00),
    ('00000000-0000-0000-0000-0000000000c1', 'DAILY', 'DAILY_RATE', 1.2500, 8.00)
ON CONFLICT DO NOTHING;
