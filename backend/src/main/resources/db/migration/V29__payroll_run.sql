-- =====================================================================
-- V29  Payroll run nucleus (FTDD Vol.1 Ch.7 / Vol.2 Ch.24).
--   A payroll RUN computes, per employee, a RESULT made of LINES
--   (the payslip). One run = one period (optionally scoped to a project).
--   Lifecycle: DRAFT -> CALCULATED -> APPROVED -> LOCKED.
--   This is slice 1: fixed pay-items + overtime (rates from the Rule
--   Engine). Leave/proration/financial-transaction deductions layer on
--   later (P5/P7). No leave handling yet by design.
-- =====================================================================

-- Per-employee daily-rate basis (how the daily rate / unpaid-day / OT base
-- is divided). Chosen on the employee file. Default GCC convention = 30.
ALTER TABLE employee
    ADD COLUMN rate_basis VARCHAR(20) NOT NULL DEFAULT 'CALENDAR_30';
-- values: CALENDAR_30 | CALENDAR_ACTUAL | WORKING_DAYS

CREATE TABLE payroll_run (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID         NOT NULL,
    period_id     UUID         NOT NULL REFERENCES payroll_period (id),
    project_id    UUID         REFERENCES project (id),       -- NULL = whole company
    pay_group     VARCHAR(20)  NOT NULL DEFAULT 'ALL',       -- ALL | DAILY | MONTHLY
    run_type      VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',     -- REGULAR | OFFCYCLE | RETRO
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',       -- DRAFT->CALCULATED->APPROVED->LOCKED
    currency_code VARCHAR(3),
    calculated_at TIMESTAMPTZ,
    approved_at   TIMESTAMPTZ,
    approved_by   VARCHAR(100),
    locked_at     TIMESTAMPTZ,
    notes         VARCHAR(500),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_payroll_run_period ON payroll_run (company_id, period_id);

CREATE TABLE payroll_result (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id       UUID          NOT NULL,
    run_id           UUID          NOT NULL REFERENCES payroll_run (id) ON DELETE CASCADE,
    employee_id      UUID          NOT NULL,
    currency_code    VARCHAR(3),
    pay_status       VARCHAR(20),                              -- MONTHLY / DAILY (snapshot)
    rate_basis       VARCHAR(20),                              -- snapshot of the basis used
    divisor          NUMERIC(6,2),                             -- the divisor actually used
    daily_rate       NUMERIC(18,4) NOT NULL DEFAULT 0,
    hourly_rate      NUMERIC(18,4) NOT NULL DEFAULT 0,
    worked_days      NUMERIC(6,2)  NOT NULL DEFAULT 0,
    normal_hours     NUMERIC(8,2)  NOT NULL DEFAULT 0,
    ot_hours         NUMERIC(8,2)  NOT NULL DEFAULT 0,
    gross            NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_earnings   NUMERIC(18,4) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(18,4) NOT NULL DEFAULT 0,
    net              NUMERIC(18,4) NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL DEFAULT 'OK',      -- OK | FLAGGED
    message          VARCHAR(500),
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(100),
    version          BIGINT        NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_payroll_result ON payroll_result (run_id, employee_id);

CREATE TABLE payroll_result_line (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID          NOT NULL,
    result_id      UUID          NOT NULL REFERENCES payroll_result (id) ON DELETE CASCADE,
    component_code VARCHAR(50),
    component_name VARCHAR(150)  NOT NULL,
    component_type VARCHAR(20)   NOT NULL,                     -- EARNING | DEDUCTION
    category       VARCHAR(40),
    quantity       NUMERIC(10,2),                              -- days / hours, if any
    rate           NUMERIC(18,4),                              -- per-unit rate, if any
    amount         NUMERIC(18,4) NOT NULL DEFAULT 0,
    source         VARCHAR(30)   NOT NULL DEFAULT 'PAY_ITEM',  -- PAY_ITEM | OVERTIME | PRORATION
    sort_order     INTEGER       NOT NULL DEFAULT 100,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT        NOT NULL DEFAULT 0
);
CREATE INDEX ix_payroll_result_line ON payroll_result_line (result_id);

