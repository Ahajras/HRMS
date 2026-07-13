-- =====================================================================
-- PREVIEW ONLY — run this first. Nothing here deletes anything.
-- Shows row counts for everything the real cleanup script would touch.
-- =====================================================================

\echo '--- Employees to be fully deleted (everyone except 77677) ---'
SELECT count(*) AS employees_to_delete FROM employee WHERE employee_number <> '77677';

\echo '--- 77677s own data to be deleted (timesheet/payroll/leave/Day Zero only) ---'
SELECT
  (SELECT count(*) FROM timesheet t JOIN employee e ON e.id=t.employee_id WHERE e.employee_number='77677') AS timesheets,
  (SELECT count(*) FROM payroll_result r JOIN employee e ON e.id=r.employee_id WHERE e.employee_number='77677') AS payroll_results,
  (SELECT count(*) FROM payroll_adjustment pa JOIN employee e ON e.id=pa.employee_id WHERE e.employee_number='77677') AS day_zero_adjustments,
  (SELECT count(*) FROM leave_request lr JOIN employee e ON e.id=lr.employee_id WHERE e.employee_number='77677') AS leave_requests;

\echo '--- Projects to be deleted (everything except the new demo project) ---'
SELECT count(*) AS projects_to_delete FROM project WHERE code <> 'DEMO';

\echo '--- Everything cascading from those employees/projects ---'
SELECT
  (SELECT count(*) FROM assignment a JOIN employee e ON e.id=a.employee_id WHERE e.employee_number <> '77677') AS assignments,
  (SELECT count(*) FROM contract c JOIN employee e ON e.id=c.employee_id WHERE e.employee_number <> '77677') AS contracts,
  (SELECT count(*) FROM crew) AS crews,
  (SELECT count(*) FROM crew_member) AS crew_members,
  (SELECT count(*) FROM timesheet t JOIN employee e ON e.id=t.employee_id WHERE e.employee_number <> '77677') AS other_timesheets,
  (SELECT count(*) FROM payroll_result r JOIN employee e ON e.id=r.employee_id WHERE e.employee_number <> '77677') AS other_payroll_results,
  (SELECT count(*) FROM payroll_run) AS payroll_runs,
  (SELECT count(*) FROM provision_run) AS provision_runs,
  (SELECT count(*) FROM ticket_ledger tl JOIN employee e ON e.id=tl.employee_id WHERE e.employee_number <> '77677') AS ticket_ledger_rows;

\echo '--- App user accounts — NONE of these will be deleted, just shown for your review ---'
SELECT username, employee_id FROM app_user;
