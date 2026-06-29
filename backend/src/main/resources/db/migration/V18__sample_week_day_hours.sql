-- =====================================================================
-- V18  Sample Week + per-day calendar hours + multi cost-code allocation
--      + extra day types  (aligns the new HRMS with the legacy CCC timesheet:
--      PAYCAL per-day normal/declared hours, PAYIN multi cost-code, CM types)
--
--   Legacy parity:
--     * shift_day = the "sample week" (per weekday: normal hours, declared OT,
--       weekly-off flag). The calendar/timesheet reads these per day instead of
--       a single standard-hours number.
--     * timesheet_day gains normal/declared/undeclared OT so the declared-vs-
--       undeclared overtime split (legacy nDec_ot / nUndec_ot) is stored.
--     * timesheet_day_cost = split a day's hours across several cost codes
--       (legacy PAYIN HR_CC1..HR_CC8).
--     * extra time types SICK / ACCIDENT / RR / UNPAID (legacy CM S/A/R/U).
-- =====================================================================

-- ---------------------------------------------------------------------
-- shift_day : the sample week for a shift. One row per day-of-week token
--   (MON,TUE,WED,THU,FRI,SAT,SUN). normal_hours + declared_ot define the
--   calendar values; weekly_off marks the rest day.
-- ---------------------------------------------------------------------
CREATE TABLE shift_day (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id      UUID         NOT NULL,
    shift_id        UUID         NOT NULL REFERENCES shift (id) ON DELETE CASCADE,
    day_of_week     VARCHAR(3)   NOT NULL,
    normal_hours    NUMERIC(5,2) NOT NULL DEFAULT 0,
    declared_ot     NUMERIC(5,2) NOT NULL DEFAULT 0,
    weekly_off      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    version         BIGINT       NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uq_shift_day ON shift_day (shift_id, day_of_week);

-- ---------------------------------------------------------------------
-- timesheet_day : richer hours breakdown (declared / undeclared OT).
-- ---------------------------------------------------------------------
ALTER TABLE timesheet_day ADD COLUMN normal_hours       NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE timesheet_day ADD COLUMN declared_ot_hours  NUMERIC(5,2) NOT NULL DEFAULT 0;
ALTER TABLE timesheet_day ADD COLUMN undeclared_ot_hours NUMERIC(5,2) NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------
-- timesheet_day_cost : split a day's worked hours across cost codes.
--   When empty, the day's own project_id/cost_code_id is the single target.
-- ---------------------------------------------------------------------
CREATE TABLE timesheet_day_cost (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timesheet_day_id UUID         NOT NULL REFERENCES timesheet_day (id) ON DELETE CASCADE,
    project_id       UUID,
    cost_code_id     UUID,
    hours            NUMERIC(5,2) NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    created_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    updated_by       VARCHAR(100),
    version          BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX ix_timesheet_day_cost_day ON timesheet_day_cost (timesheet_day_id);

-- ---------------------------------------------------------------------
-- Extra day/time types for the seeded company (legacy CM codes S/A/R/U).
-- ---------------------------------------------------------------------
INSERT INTO time_type (company_id, code, name, category, paid, counts_as_worked, affects_leave, factor, sort_order)
VALUES
    ('00000000-0000-0000-0000-0000000000c1', 'SICK',     'Sick leave',       'SICK',     TRUE,  FALSE, FALSE, 1.000, 35),
    ('00000000-0000-0000-0000-0000000000c1', 'ACCIDENT', 'Work accident',    'ACCIDENT', TRUE,  FALSE, FALSE, 1.000, 36),
    ('00000000-0000-0000-0000-0000000000c1', 'RR',       'Rest & recreation','RR',       TRUE,  FALSE, TRUE,  1.000, 55),
    ('00000000-0000-0000-0000-0000000000c1', 'UNPAID',   'Unpaid leave',     'UNPAID',   FALSE, FALSE, FALSE, 0.000, 56);

-- ---------------------------------------------------------------------
-- Seed the sample week for the seeded DAY shift: Sat-Thu 8h normal + 2h
-- declared OT, Friday weekly-off. (Gulf week.)
-- ---------------------------------------------------------------------
INSERT INTO shift_day (company_id, shift_id, day_of_week, normal_hours, declared_ot, weekly_off)
SELECT s.company_id, s.id, d.dow, d.nh, d.ot, d.off
FROM shift s
CROSS JOIN (VALUES
    ('SAT', 8.0, 2.0, FALSE),
    ('SUN', 8.0, 2.0, FALSE),
    ('MON', 8.0, 2.0, FALSE),
    ('TUE', 8.0, 2.0, FALSE),
    ('WED', 8.0, 2.0, FALSE),
    ('THU', 8.0, 2.0, FALSE),
    ('FRI', 0.0, 0.0, TRUE)
) AS d(dow, nh, ot, off)
WHERE s.company_id = '00000000-0000-0000-0000-0000000000c1' AND s.code = 'DAY';
