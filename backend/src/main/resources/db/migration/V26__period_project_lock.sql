-- =====================================================================
-- V26  Per-project lock within a payroll period.
--   A period stays one calendar month, but each PROJECT can be locked
--   independently: you can lock project A (ready for payroll) while
--   project B keeps collecting timesheets.
--   status: OPEN (editable) -> LOCKED (frozen, ready for payroll) -> CLOSED.
-- =====================================================================
CREATE TABLE payroll_period_project (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID         NOT NULL,
    period_id   UUID         NOT NULL REFERENCES payroll_period (id) ON DELETE CASCADE,
    project_id  UUID         NOT NULL REFERENCES project (id),
    status      VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    locked_at   TIMESTAMPTZ,
    closed_at   TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    version     BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_period_project ON payroll_period_project (period_id, project_id);
