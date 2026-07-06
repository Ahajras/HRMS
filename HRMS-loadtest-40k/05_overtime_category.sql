-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 5: Overtime category + band
-- Without this, employees have no overtime_category_code, so the OT
-- eligibility gate in the payroll engine has nothing to key off.
-- 90% get an OT-eligible category, 10% get a non-eligible one (e.g.
-- management) — a realistic mix that exercises BOTH code paths:
-- normal paid OT, and the "ineligible OT" hidden-hours path.
-- =====================================================================

UPDATE employee e
SET overtime_category_code = CASE WHEN substring(e.employee_number from 4)::int % 10 = 0 THEN '01' ELSE '04' END,
    band = lpad((((substring(e.employee_number from 4)::int - 1) % 10) + 1)::text, 2, '0')
WHERE e.employee_number LIKE 'LT-%';
