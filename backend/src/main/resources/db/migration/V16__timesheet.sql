-- =====================================================================
-- V16  Timesheet / Calendar & Shift / Time Type  (FTDD Vol.1 Ch.3,4,5)
--   The Timesheet module is the source of ACTUAL worked hours. Every later
--   engine (overtime, leave, payroll, settlement) reads from here.
--   Design boundaries:
--     * Shift CLASSIFIES the day (regular / weekly-off / holiday).
--     * Rule Engine owns the RATES (e.g. OT multipliers) — not stored here.
--     * Time Type is a configurable classification with a paid flag + factor.
--   Lifecycle: DRAFT -> SUBMITTED -> APPROVED -> LOCKED (reopen sends it back
--   to DRAFT and invalidates the approval).
-- =====================================================================

-- ---------------------------------------------------------------------
-- shift : a working schedule, effective-dated/versioned. weekly_off is a
--   comma-separated set of day-of-week tokens (MON,TUE,WED,THU,FRI,SAT,SUN).
-- ---------------------------------------------------------------------
CREATE TABLE shift (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL,
    code            VARCHAR(30)  NOT NULL,
    name            VARCHAR(150) NOT NULL,
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    break_minutes   INTEGER      NOT NULL DEFAULT 0,
    standard_hours  NUMERIC(5,2) NOT NULL,
    crosses_midnight BOOLEAN     NOT NULL DEFAULT FALSE,
    weekly_off      VARCHAR(40),
    effective_from  DATE         NOT NULL DEFAULT DATE '2020-01-01',
    effective_to    DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_shift_dates CHECK (effective_to IS NULL OR effective_to >= effective_from)
);
CREATE UNIQUE INDEX uq_shift_company_code ON shift (company_id, code);

-- ---------------------------------------------------------------------
-- public_holiday : company calendar of non-working paid days.
-- ---------------------------------------------------------------------
CREATE TABLE public_holiday (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID         NOT NULL,
    holiday_date  DATE         NOT NULL,
    name          VARCHAR(150) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_public_holiday ON public_holiday (company_id, holiday_date);

-- ---------------------------------------------------------------------
-- time_type : configurable day/segment classification. factor is a
--   reference multiplier; authoritative pay rates live in the Rule Engine.
-- ---------------------------------------------------------------------
CREATE TABLE time_type (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    name                VARCHAR(150) NOT NULL,
    category            VARCHAR(30)  NOT NULL,
    paid                BOOLEAN      NOT NULL DEFAULT TRUE,
    counts_as_worked    BOOLEAN      NOT NULL DEFAULT TRUE,
    affects_leave       BOOLEAN      NOT NULL DEFAULT FALSE,
    factor              NUMERIC(6,3) NOT NULL DEFAULT 1.000,
    sort_order          INTEGER      NOT NULL DEFAULT 0,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_time_type_company_code ON time_type (company_id, code);

-- ---------------------------------------------------------------------
-- timesheet : one per employee per calendar month. Header carries the
--   default shift and the rolled-up totals.
-- ---------------------------------------------------------------------
CREATE TABLE timesheet (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id          UUID         NOT NULL,
    employee_id         UUID         NOT NULL REFERENCES employee (id),
    period_year         INTEGER      NOT NULL,
    period_month        INTEGER      NOT NULL,
    shift_id            UUID         REFERENCES shift (id),
    status              VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    total_worked_hours  NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_ot_hours      NUMERIC(8,2) NOT NULL DEFAULT 0,
    total_absence_days  NUMERIC(6,2) NOT NULL DEFAULT 0,
    submitted_at        TIMESTAMPTZ,
    approved_at         TIMESTAMPTZ,
    approved_by         VARCHAR(100),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    version             BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT ck_timesheet_month CHECK (period_month BETWEEN 1 AND 12)
);
CREATE UNIQUE INDEX uq_timesheet_emp_period ON timesheet (company_id, employee_id, period_year, period_month);
CREATE INDEX ix_timesheet_period ON timesheet (company_id, period_year, period_month);

-- ---------------------------------------------------------------------
-- timesheet_day : one row per calendar day of the period.
-- ---------------------------------------------------------------------
CREATE TABLE timesheet_day (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timesheet_id    UUID         NOT NULL REFERENCES timesheet (id) ON DELETE CASCADE,
    work_date       DATE         NOT NULL,
    shift_id        UUID         REFERENCES shift (id),
    time_type_id    UUID         REFERENCES time_type (id),
    planned_hours   NUMERIC(5,2) NOT NULL DEFAULT 0,
    actual_in       TIME,
    actual_out      TIME,
    worked_hours    NUMERIC(5,2) NOT NULL DEFAULT 0,
    ot_hours        NUMERIC(5,2) NOT NULL DEFAULT 0,
    project_id      UUID,
    cost_code_id    UUID,
    remarks         VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_timesheet_day ON timesheet_day (timesheet_id, work_date);

-- ---------------------------------------------------------------------
-- Seed defaults for the seeded company (00000000-...c1).
-- ---------------------------------------------------------------------
INSERT INTO shift (company_id, code, name, start_time, end_time, break_minutes, standard_hours, weekly_off)
VALUES ('00000000-0000-0000-0000-0000000000c1', 'DAY', 'Standard Day Shift',
        TIME '08:00', TIME '17:00', 60, 8.00, 'FRI');

INSERT INTO time_type (company_id, code, name, category, paid, counts_as_worked, affects_leave, factor, sort_order)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'REGULAR',  'Regular work',     'REGULAR',  TRUE,  TRUE,  FALSE, 1.000, 10),
    ('00000000-0000-0000-0000-0000000000c1', 'OVERTIME', 'Overtime',         'OVERTIME', TRUE,  TRUE,  FALSE, 1.250, 20),
    ('00000000-0000-0000-0000-0000000000c1', 'REST',     'Weekly rest day',  'REST',     TRUE,  FALSE, FALSE, 1.000, 30),
    ('00000000-0000-0000-0000-0000000000c1', 'HOLIDAY',  'Public holiday',   'HOLIDAY',  TRUE,  FALSE, FALSE, 1.000, 40),
    ('00000000-0000-0000-0000-0000000000c1', 'ABSENCE',  'Unpaid absence',   'ABSENCE',  FALSE, FALSE, FALSE, 0.000, 50),
    ('00000000-0000-0000-0000-0000000000c1', 'LEAVE',    'Paid leave',       'LEAVE',    TRUE,  FALSE, TRUE,  1.000, 60);
