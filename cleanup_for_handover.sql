-- =====================================================================
-- HANDOVER CLEANUP — irreversible. Take a full database backup first.
--
--   Kept, untouched:  every app_user login account (Users screen)
--   Kept, untouched:  employee 77677's employee/contract/assignment record
--   Wiped for 77677:  timesheet, payroll results, Day Zero, leave requests
--   Wiped entirely:   every OTHER employee + everything tied to them
--   Wiped entirely:   every project except a newly created "DEMO" project
--   77677 gets reassigned to the DEMO project.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- PART 1 — create the DEMO project, move 77677 onto it
-- ---------------------------------------------------------------------
INSERT INTO project (id, company_id, code, name, status)
SELECT gen_random_uuid(), e.company_id, 'DEMO', 'Demo Project', 'ACTIVE'
FROM employee e
WHERE e.employee_number = '77677'
  AND NOT EXISTS (SELECT 1 FROM project p WHERE p.code = 'DEMO' AND p.company_id = e.company_id);

UPDATE assignment a
SET project_id = p.id,
    cost_code_id = NULL
FROM employee e, project p
WHERE a.employee_id = e.id
  AND e.employee_number = '77677'
  AND p.code = 'DEMO'
  AND p.company_id = e.company_id;

-- ---------------------------------------------------------------------
-- PART 2 — wipe 77677's OWN timesheet / payroll / leave / Day Zero,
-- but keep the employee, contract, and assignment record itself.
-- ---------------------------------------------------------------------
DELETE FROM timesheet_day_cost tdc
USING timesheet_day td, timesheet t, employee e
WHERE tdc.timesheet_day_id = td.id AND td.timesheet_id = t.id
  AND t.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM timesheet_day td
USING timesheet t, employee e
WHERE td.timesheet_id = t.id AND t.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM payroll_result_line prl
USING payroll_result pr, employee e
WHERE prl.result_id = pr.id AND pr.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM payroll_result pr
USING employee e
WHERE pr.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM payroll_adjustment pa
USING employee e
WHERE pa.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM leave_adjustment la
USING employee e
WHERE la.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM leave_request lr
USING employee e
WHERE lr.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM ticket_ledger tl
USING employee e
WHERE tl.employee_id = e.id AND e.employee_number = '77677';

DELETE FROM timesheet t
USING employee e
WHERE t.employee_id = e.id AND e.employee_number = '77677';

-- ---------------------------------------------------------------------
-- PART 3 — batch calculation headers (payroll runs, provisions) are
-- period/company-wide artifacts, not one employee's data. Wipe them
-- entirely for a clean slate (their own child rows first).
-- ---------------------------------------------------------------------
DELETE FROM payroll_result_line;
DELETE FROM payroll_result;
DELETE FROM payroll_run;
DELETE FROM provision_result;
DELETE FROM provision_run;
DELETE FROM ticket_ledger;

-- ---------------------------------------------------------------------
-- PART 4 — delete every OTHER employee and everything tied to them.
-- ---------------------------------------------------------------------
DELETE FROM app_user au USING employee e
WHERE au.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM crew_member cm USING employee e
WHERE cm.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM employee_bank_account eb USING employee e
WHERE eb.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM employee_dependent ed USING employee e
WHERE ed.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM employee_document edoc USING employee e
WHERE edoc.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM employee_shift es USING employee e
WHERE es.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM timekeeper_project tp USING employee e
WHERE tp.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM legacy_employee_raw ler USING employee e
WHERE ler.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM contract_pay_item cpi USING employee e
WHERE cpi.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM contract c USING employee e
WHERE c.employee_id = e.id AND e.employee_number <> '77677';

DELETE FROM assignment a USING employee e
WHERE a.employee_id = e.id AND e.employee_number <> '77677';

-- Clear self-referencing links (supervisor/timekeeper) before the delete.
UPDATE employee SET supervisor_employee_id = NULL, timekeeper_employee_id = NULL
WHERE employee_number <> '77677';
UPDATE employee SET supervisor_employee_id = NULL
WHERE employee_number = '77677' AND supervisor_employee_id IN (
    SELECT id FROM employee WHERE employee_number <> '77677'
);

DELETE FROM employee WHERE employee_number <> '77677';

-- ---------------------------------------------------------------------
-- PART 5 — delete every project except DEMO, and everything project-
-- scoped tied to those old projects.
-- ---------------------------------------------------------------------
UPDATE crew SET parent_crew_id = NULL WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM crew_trade ct USING crew c WHERE ct.crew_id = c.id AND c.project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM crew_member cm USING crew c WHERE cm.crew_id = c.id AND c.project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM crew WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');

DELETE FROM payroll_period_project WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM shift WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM cost_code WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM payroll_rule WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');
DELETE FROM provision_rule WHERE project_id IN (SELECT id FROM project WHERE code <> 'DEMO');

DELETE FROM project WHERE code <> 'DEMO';

COMMIT;

-- ---------------------------------------------------------------------
-- Verify
-- ---------------------------------------------------------------------
SELECT (SELECT count(*) FROM employee) AS employees_left,
       (SELECT count(*) FROM project) AS projects_left,
       (SELECT count(*) FROM app_user) AS app_users_left,
       (SELECT count(*) FROM timesheet) AS timesheets_left,
       (SELECT count(*) FROM payroll_result) AS payroll_results_left;
