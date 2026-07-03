-- =====================================================================
-- V35  Configurable thresholds for time-type payroll effects.
--   Adds three columns to time_type_payroll_rule so ANY rule can say
--   "apply this effect only AFTER N days" — consecutive (spell) or annual.
--   Adds the general threshold NUMBERS to the Country Law rule set so they
--   are editable from the Country Law screen (no hardcoding).
--
--   SAFE: only ADD COLUMN / INSERT new rows. Does NOT touch existing data
--   (your 68 linkage rows, your time_type classifications, employees, etc.).
-- =====================================================================

-- 1) Threshold behaviour, per linkage row -----------------------------
--    threshold_days   : effect starts only after this many qualifying days
--                       (0 = immediate, i.e. current behaviour — the default).
--    threshold_scope  : how the days are counted
--                         CONSECUTIVE = one unbroken spell (off days bridge, not counted)
--                         ANNUAL      = accumulated over the year
--                         NONE        = no threshold (immediate) [default]
--    year_basis       : for ANNUAL scope, when the yearly counter resets
--                         CALENDAR  = 1 January [default]
--                         HIRE_DATE = employee's hire anniversary
ALTER TABLE time_type_payroll_rule
    ADD COLUMN threshold_days  INTEGER     NOT NULL DEFAULT 0;
ALTER TABLE time_type_payroll_rule
    ADD COLUMN threshold_scope VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE time_type_payroll_rule
    ADD COLUMN year_basis      VARCHAR(20) NOT NULL DEFAULT 'CALENDAR';

-- 2) General threshold values in Country Law (editable from the screen) -
--    Seeded only into the global QATAR package (company_id IS NULL), the
--    same package V14 seeded. Skipped automatically if a code already exists.
INSERT INTO rule (package_id, code, category, name, value_type, value_number, unit, effective_from, description)
SELECT p.id, v.code, v.category, v.name, v.value_type, v.value_number, v.unit, DATE '2020-01-01', v.description
FROM rule_package p
JOIN (VALUES
    ('SICK_REDUCED_PAY_PERCENT',            'LEAVE',     'Sick leave pay % after full-pay days', 'PERCENT', 50, '%',    'Percentage of pay after the full-pay sick days are used'),
    ('ALLOWANCE_STOP_AFTER_UNPAID_DAYS',    'ALLOWANCE', 'Stop allowances after unpaid days',    'INTEGER', 7,  'days', 'Consecutive unpaid-leave days after which affected allowances stop'),
    ('SPECIAL_SUBSIDY_STOP_AFTER_DAYS',     'ALLOWANCE', 'Stop special family subsidy after',    'INTEGER', 30, 'days', 'Consecutive unpaid-leave days after which the special family subsidy stops'),
    ('BRIDGING_DAY_TYPES',                   'ALLOWANCE', 'Day types that bridge a spell',        'TEXT',    NULL, NULL,  'Time-type codes that do NOT break a consecutive count (others break it)')
) AS v(code, category, name, value_type, value_number, unit, description) ON TRUE
WHERE p.company_id IS NULL AND p.code = 'QATAR'
  AND NOT EXISTS (
        SELECT 1 FROM rule r WHERE r.package_id = p.id AND r.code = v.code
  );

-- Bridging day types default value (text): weekend + holiday bridge; others break.
UPDATE rule SET value_text = 'W,H'
WHERE code = 'BRIDGING_DAY_TYPES' AND value_text IS NULL
  AND package_id IN (SELECT id FROM rule_package WHERE company_id IS NULL AND code = 'QATAR');
