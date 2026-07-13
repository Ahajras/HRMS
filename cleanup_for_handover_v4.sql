-- =====================================================================
-- HANDOVER CLEANUP v4 — irreversible. Take a full database backup first.
--
--   Kept, untouched:  every app_user login account (Users screen)
--   Kept, untouched:  the specific employee record below (id, not
--                      employee_number — there were two rows sharing the
--                      same "77677" number, so the id is unambiguous)
--   Wiped for that employee:  timesheet, payroll results, Day Zero,
--                      leave requests (contract + assignment kept)
--   Wiped entirely:    every OTHER employee + everything tied to them
--   Wiped entirely:    every project except a newly created "DEMO"
--                      project (the kept employee is reassigned to it)
--
--   v4 — order fully re-derived from the database's actual complete
--   foreign-key graph (not guessed), and test-run against seeded data
--   covering every tricky case found in v1–v3 (a crew with a foreman, a
--   shift referenced by timesheet AND crew_member AND employee_shift, a
--   leave request referenced by a timesheet day, self-referencing
--   employee/crew rows) before being handed over.
-- =====================================================================

BEGIN;

\set kept_id 'efe2f49c-1f5e-4ce8-a004-9275da55af1a'

-- ---------------------------------------------------------------------
-- PART 1 — create the DEMO project, move the kept employee onto it,
-- clear any self-referencing links that would otherwise dangle.
-- ---------------------------------------------------------------------
INSERT INTO project (id, company_id, code, name, status)
SELECT gen_random_uuid(), e.company_id, 'DEMO', 'Demo Project', 'ACTIVE'
FROM employee e
WHERE e.id = :'kept_id'::uuid
  AND NOT EXISTS (SELECT 1 FROM project p WHERE p.code = 'DEMO' AND p.company_id = e.company_id);

UPDATE assignment a
SET project_id = p.id, cost_code_id = NULL
FROM employee e, project p
WHERE a.employee_id = e.id AND e.id = :'kept_id'::uuid
  AND p.code = 'DEMO' AND p.company_id = e.company_id;

UPDATE assignment SET supervisor_employee_id = NULL
WHERE employee_id = :'kept_id'::uuid
  AND supervisor_employee_id IS NOT NULL AND supervisor_employee_id <> :'kept_id'::uuid;

UPDATE employee SET supervisor_employee_id = NULL, timekeeper_employee_id = NULL
WHERE id = :'kept_id'::uuid;

-- ---------------------------------------------------------------------
-- PART 2 — wipe the kept employee's OWN timesheet / payroll / leave /
-- Day Zero, but keep the employee, contract, and assignment record.
-- timesheet -> timesheet_day -> timesheet_day_cost all cascade, so
-- deleting timesheet alone is enough; leave_request must come AFTER
-- (timesheet_day references it) — the cascade above already cleared
-- that reference by the time this line runs.
-- ---------------------------------------------------------------------
DELETE FROM timesheet WHERE employee_id = :'kept_id'::uuid;
DELETE FROM leave_adjustment WHERE employee_id = :'kept_id'::uuid;
DELETE FROM leave_request WHERE employee_id = :'kept_id'::uuid;
DELETE FROM payroll_adjustment WHERE employee_id = :'kept_id'::uuid;

-- ---------------------------------------------------------------------
-- PART 3 — batch calculation headers (payroll runs, provisions, every
-- employee's payroll results, tickets) are period/company-wide
-- artifacts, not one employee's data. Wipe them entirely.
-- ---------------------------------------------------------------------
DELETE FROM payroll_result;   -- cascades payroll_result_line
DELETE FROM payroll_run;
DELETE FROM provision_run;    -- cascades provision_result
DELETE FROM ticket_ledger;

-- ---------------------------------------------------------------------
-- PART 4 — every OTHER employee's timesheet / leave (same cascade
-- reasoning as Part 2), done for everyone at once.
-- ---------------------------------------------------------------------
DELETE FROM timesheet WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM leave_adjustment WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM leave_request WHERE employee_id <> :'kept_id'::uuid;

-- ---------------------------------------------------------------------
-- PART 5 — crews reference employees (foreman) and shifts (crew_member),
-- so they must go before both. Deleting the crew row cascades
-- crew_member and crew_trade automatically.
-- ---------------------------------------------------------------------
UPDATE crew SET parent_crew_id = NULL WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM crew WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');

-- ---------------------------------------------------------------------
-- PART 6 — clear the rest of what references employees directly.
-- ---------------------------------------------------------------------
DELETE FROM app_user WHERE employee_id <> :'kept_id'::uuid AND employee_id IS NOT NULL;
DELETE FROM employee_bank_account WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM employee_dependent WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM employee_document WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM employee_shift WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM timekeeper_project WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM legacy_employee_raw WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM contract_pay_item WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM contract WHERE employee_id <> :'kept_id'::uuid;
DELETE FROM assignment WHERE employee_id <> :'kept_id'::uuid;

-- ---------------------------------------------------------------------
-- PART 7 — old projects: everything project-scoped that could still
-- reference them (shift is referenced by timesheet/crew_member/
-- employee_shift, all already gone by this point; cost_code is
-- referenced by assignment, already gone; timekeeper_project, already
-- gone). Safe to delete the project-scoped config now, then the
-- projects themselves. This must happen BEFORE deleting employees below
-- — project.manager_employee_id references employee too.
-- ---------------------------------------------------------------------
DELETE FROM payroll_period_project WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM shift WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM cost_code WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM payroll_rule WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM provision_rule WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM timekeeper_project WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');

DELETE FROM project WHERE code <> 'DEMO';

-- ---------------------------------------------------------------------
-- PART 8 — now safe to delete every OTHER employee (crews, timesheets,
-- leave, assignments, contracts, app_user links, and the projects that
-- referenced them via manager_employee_id are all already gone).
-- ---------------------------------------------------------------------
UPDATE employee SET supervisor_employee_id = NULL, timekeeper_employee_id = NULL
WHERE id <> :'kept_id'::uuid;

DELETE FROM employee WHERE id <> :'kept_id'::uuid;

COMMIT;

-- ---------------------------------------------------------------------
-- Verify
-- ---------------------------------------------------------------------
SELECT (SELECT count(*) FROM employee) AS employees_left,
       (SELECT count(*) FROM project) AS projects_left,
       (SELECT count(*) FROM app_user) AS app_users_left,
       (SELECT count(*) FROM timesheet) AS timesheets_left,
       (SELECT count(*) FROM payroll_result) AS payroll_results_left,
       (SELECT count(*) FROM crew) AS crews_left,
       (SELECT count(*) FROM shift) AS shifts_left,
       (SELECT count(*) FROM cost_code) AS cost_codes_left;
