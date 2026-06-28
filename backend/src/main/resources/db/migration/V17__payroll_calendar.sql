-- =====================================================================
-- V17  Payroll Calendar / Period / Week  +  Employee-Shift roster
--      (FTDD Vol.1 Ch.4 Calendar & Shift Engine)
--
--   The Calendar is the backbone of time & payroll. A company has a
--   calendar; from it we GENERATE concrete periods (one per month) and the
--   weeks inside each period. A timesheet always lives inside a period, and
--   payroll can only be processed once the period is LOCKED/CLOSED.
--
--   Period lifecycle:  OPEN -> LOCKED -> CLOSED
--     OPEN   : timesheets can be generated / edited / approved.
--     LOCKED : cutoff. No timesheet edits. Ready for payroll. (reopen -> OPEN)
--     CLOSED : payroll posted. Final.
--
--   Weeks matter because overtime is computed on a weekly basis (Gulf labor
--   law: hours beyond the weekly threshold are overtime).
-- =====================================================================

-- ---------------------------------------------------------------------
-- payroll_calendar : one (or more) per company. frequency is MONTHLY for
--   now; week_start is the day-of-week each week begins on (e.g. SAT).
-- ---------------------------------------------------------------------
CREATE TABLE payroll_calendar (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id   UUID         NOT NULL,
    code         VARCHAR(30)  NOT NULL,
    name         VARCHAR(150) NOT NULL,
    frequency    VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    week_start   VARCHAR(3)   NOT NULL DEFAULT 'SAT',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by   VARCHAR(100),
    updated_at   TIMESTAMPTZ,
    updated_by   VARCHAR(100),
    version      BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_payroll_calendar_company_code ON payroll_calendar (company_id, code);

-- ---------------------------------------------------------------------
-- payroll_period : one calendar month. Carries its date range, pay date
--   and the OPEN/LOCKED/CLOSED status.
-- ---------------------------------------------------------------------
CREATE TABLE payroll_period (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID         NOT NULL,
    calendar_id   UUID         NOT NULL REFERENCES payroll_calendar (id) ON DELETE CASCADE,
    period_year   INTEGER      NOT NULL,
    period_month  INTEGER      NOT NULL,
    name          VARCHAR(50)  NOT NULL,
    start_date    DATE         NOT NULL,
    end_date      DATE         NOT NULL,
    pay_date      DATE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    locked_at     TIMESTAMPTZ,
    closed_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_payroll_period_month CHECK (period_month BETWEEN 1 AND 12)
);
CREATE UNIQUE INDEX uq_payroll_period ON payroll_period (company_id, calendar_id, period_year, period_month);
CREATE INDEX ix_payroll_period_year ON payroll_period (company_id, period_year);

-- ---------------------------------------------------------------------
-- payroll_week : the weeks that make up a period (for weekly OT).
-- ---------------------------------------------------------------------
CREATE TABLE payroll_week (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id  UUID        NOT NULL,
    period_id   UUID        NOT NULL REFERENCES payroll_period (id) ON DELETE CASCADE,
    week_no     INTEGER     NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by  VARCHAR(100),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    version     BIGINT      NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_payroll_week ON payroll_week (period_id, week_no);

-- ---------------------------------------------------------------------
-- employee_shift : roster — which shift an employee works, effective-dated.
--   Timesheet generation resolves the shift from here when none is passed.
-- ---------------------------------------------------------------------
CREATE TABLE employee_shift (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id     UUID        NOT NULL,
    employee_id    UUID        NOT NULL REFERENCES employee (id),
    shift_id       UUID        NOT NULL REFERENCES shift (id),
    effective_from DATE        NOT NULL,
    effective_to   DATE,
    status         VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    version        BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT ck_employee_shift_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE INDEX ix_employee_shift_emp ON employee_shift (company_id, employee_id, effective_from);

-- ---------------------------------------------------------------------
-- Link the timesheet to its period (nullable for any pre-existing rows).
-- ---------------------------------------------------------------------
ALTER TABLE timesheet ADD COLUMN period_id UUID REFERENCES payroll_period (id);

-- ---------------------------------------------------------------------
-- Seed a default monthly calendar for the seeded company (00000000-...c1).
-- Periods/weeks are generated on demand via the "Create Calendar (year)"
-- action, so a fresh install can build any year (including historical ones).
-- ---------------------------------------------------------------------
INSERT INTO payroll_calendar (company_id, code, name, frequency, week_start)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'DEFAULT', 'Default Monthly Calendar', 'MONTHLY', 'SAT');
