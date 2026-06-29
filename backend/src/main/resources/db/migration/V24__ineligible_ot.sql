-- =====================================================================
-- V24  Record ineligible overtime separately instead of dropping it.
--   When an employee's overtime category is NOT eligible, hours worked
--   beyond normal are stored here (NOT paid, NOT shown to the employee in
--   reports/payroll) — kept only as an internal record.
-- =====================================================================
ALTER TABLE timesheet_day ADD COLUMN ineligible_ot_hours NUMERIC(5,2) NOT NULL DEFAULT 0;
