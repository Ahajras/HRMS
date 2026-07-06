-- =====================================================================
-- LOAD TEST CLEANUP — removes everything tagged 'LT-%' / 'LT-P%'
-- Deletes in the correct order to respect foreign keys.
-- Run this after the test is done to fully restore the database.
-- =====================================================================

DELETE FROM timesheet_day WHERE timesheet_id IN (
    SELECT id FROM timesheet WHERE employee_id IN (
        SELECT id FROM employee WHERE employee_number LIKE 'LT-%'));

DELETE FROM timesheet WHERE employee_id IN (
    SELECT id FROM employee WHERE employee_number LIKE 'LT-%');

DELETE FROM employee_shift WHERE employee_id IN (
    SELECT id FROM employee WHERE employee_number LIKE 'LT-%');

DELETE FROM contract_pay_item WHERE employee_id IN (
    SELECT id FROM employee WHERE employee_number LIKE 'LT-%');

DELETE FROM contract WHERE employee_id IN (
    SELECT id FROM employee WHERE employee_number LIKE 'LT-%');

DELETE FROM assignment WHERE employee_id IN (
    SELECT id FROM employee WHERE employee_number LIKE 'LT-%');

DELETE FROM employee WHERE employee_number LIKE 'LT-%';

-- Reference data created for the test (safe to remove; harmless to keep too)
DELETE FROM payroll_rule WHERE project_id IN (SELECT id FROM project WHERE code LIKE 'LT-P%');
DELETE FROM shift_day WHERE shift_id IN (SELECT id FROM shift WHERE code LIKE 'LT-SHIFT-%');
DELETE FROM shift WHERE code LIKE 'LT-SHIFT-%';
-- NOTE: we deliberately do NOT delete the company-wide default (project_id IS NULL)
-- MONTHLY/DAILY rules here — on a real server those almost certainly already existed
-- before this test ran, and deleting them would break normal payroll for the whole
-- company. Leaving a default rule behind if the test happened to create one on an
-- empty environment is harmless.
DELETE FROM project WHERE code LIKE 'LT-P%';
DELETE FROM payroll_component WHERE code LIKE 'LT-%';
DELETE FROM organization_unit WHERE code = 'LT-ORG';
DELETE FROM org_unit_type WHERE code = 'LT-TYPE';

-- Final check — should all return 0
SELECT
  (SELECT count(*) FROM employee WHERE employee_number LIKE 'LT-%') AS employees_left,
  (SELECT count(*) FROM project WHERE code LIKE 'LT-P%') AS projects_left,
  (SELECT count(*) FROM payroll_component WHERE code LIKE 'LT-%') AS components_left;
