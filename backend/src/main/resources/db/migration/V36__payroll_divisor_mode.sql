-- =====================================================================
-- V36  Monthly divisor mode: fixed value vs actual month days.
--   FIXED        = always divide by month_divisor (e.g. 30), so a monthly
--                  salary is the same every month regardless of 28/30/31.
--   ACTUAL_MONTH = use the real number of days in the month.
--   Default FIXED (the correct behaviour for fixed monthly salaries).
--   SAFE: only ADD COLUMN with a default; existing rows are untouched.
-- =====================================================================
ALTER TABLE payroll_rule
    ADD COLUMN divisor_mode VARCHAR(20) NOT NULL DEFAULT 'FIXED';
