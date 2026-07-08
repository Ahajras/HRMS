-- =====================================================================
-- V47  Day Zero — early payroll close with automatic next-period correction
--
--   payroll_rule.day_zero_cutoff_day : if set (e.g. 22), any day AFTER this
--     day-of-month gets marked "estimated" when its project is locked —
--     it was paid on a default assumption because the real month hadn't
--     finished yet.
--
--   timesheet_day.estimated : true for a day that was paid on that default
--     assumption rather than a confirmed attendance/leave record.
--
--   payroll_adjustment : a pending correction, created when something
--     (e.g. an approved leave) turns out to affect an ESTIMATED day whose
--     period is already locked. We never reopen the locked period — the
--     difference sits here until the next payroll run picks it up.
-- =====================================================================

ALTER TABLE payroll_rule ADD COLUMN IF NOT EXISTS day_zero_cutoff_day INT;

ALTER TABLE timesheet_day ADD COLUMN IF NOT EXISTS estimated BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS payroll_adjustment (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID          NOT NULL,
    employee_id         UUID          NOT NULL,
    work_date           DATE          NOT NULL,
    original_period_id  UUID          NOT NULL,
    reason              VARCHAR(255)  NOT NULL,
    amount              NUMERIC(14,2) NOT NULL,
    source              VARCHAR(20)   NOT NULL DEFAULT 'SYSTEM',
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    applied_run_id      UUID,
    applied_at          TIMESTAMPTZ,
    version             BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS ix_payroll_adjustment_pending
    ON payroll_adjustment (company_id, employee_id, status);
