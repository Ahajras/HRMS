-- =====================================================================
-- V56  Day Zero — re-include rest/holiday days as "estimated".
--
--   V54 excluded rest/holiday days on the theory that their schedule is
--   known in advance so nothing is "uncertain" about them. That reasoning
--   was incomplete: the day TYPE (rest day) is known, but whether the
--   employee actually showed up and worked overtime on that rest day is
--   NOT known until later — exactly the kind of thing Day Zero exists to
--   correct. This re-marks rest/holiday days as estimated for periods
--   that are already locked, using the same cutoff-day logic as the
--   normal lock-time marking (but checked against LOCKED status, since
--   these timesheets have already passed through APPROVED -> LOCKED).
-- =====================================================================

UPDATE timesheet_day td
SET estimated = true
FROM timesheet t, employee e, assignment a
WHERE t.id = td.timesheet_id
  AND e.id = t.employee_id
  AND a.employee_id = t.employee_id
  AND upper(coalesce(a.status, '')) = 'ACTIVE'
  AND a.primary_assignment = true
  AND a.effective_to IS NULL
  AND t.status = 'LOCKED'
  AND extract(day FROM td.work_date)::int > coalesce(
        (SELECT r.day_zero_cutoff_day FROM payroll_rule r
           WHERE r.company_id = t.company_id AND r.project_id = a.project_id
             AND r.pay_group = e.pay_status AND r.status = 'ACTIVE' LIMIT 1),
        (SELECT r.day_zero_cutoff_day FROM payroll_rule r
           WHERE r.company_id = t.company_id AND r.project_id IS NULL
             AND r.pay_group = e.pay_status AND r.status = 'ACTIVE' LIMIT 1),
        999);
