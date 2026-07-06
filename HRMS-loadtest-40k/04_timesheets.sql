-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 4: Timesheets
-- Generates one APPROVED timesheet per employee for the current
-- period, with a daily record for every day of the month (Friday = W,
-- all other days = N). This is deliberately simple — the goal here is
-- to measure PERFORMANCE at scale, not to re-test rule correctness
-- (that was already validated on real payslip examples earlier).
-- =====================================================================

INSERT INTO timesheet (company_id, employee_id, period_year, period_month, shift_id, status)
SELECT
    '00000000-0000-0000-0000-0000000000c1',
    e.id,
    EXTRACT(YEAR FROM CURRENT_DATE)::int,
    EXTRACT(MONTH FROM CURRENT_DATE)::int,
    (SELECT id FROM shift WHERE company_id = '00000000-0000-0000-0000-0000000000c1' LIMIT 1),
    'APPROVED'
FROM employee e
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (
      SELECT 1 FROM timesheet t WHERE t.employee_id = e.id
        AND t.period_year = EXTRACT(YEAR FROM CURRENT_DATE)::int
        AND t.period_month = EXTRACT(MONTH FROM CURRENT_DATE)::int
  );

INSERT INTO timesheet_day (timesheet_id, work_date, shift_id, time_type_id, planned_hours, actual_in, actual_out, worked_hours, ot_hours, project_id)
SELECT
    t.id,
    d.work_date,
    t.shift_id,
    (SELECT id FROM time_type WHERE company_id = '00000000-0000-0000-0000-0000000000c1'
        AND code = CASE WHEN EXTRACT(DOW FROM d.work_date) = 5 THEN 'W' ELSE 'N' END),
    CASE WHEN EXTRACT(DOW FROM d.work_date) = 5 THEN 0 ELSE 8 END,
    CASE WHEN EXTRACT(DOW FROM d.work_date) = 5 THEN NULL ELSE TIME '08:00' END,
    CASE WHEN EXTRACT(DOW FROM d.work_date) = 5 THEN NULL ELSE TIME '17:00' END,
    CASE WHEN EXTRACT(DOW FROM d.work_date) = 5 THEN 0 ELSE 8 END,
    0,
    a.project_id
FROM timesheet t
JOIN employee e ON e.id = t.employee_id
JOIN assignment a ON a.employee_id = e.id
CROSS JOIN LATERAL generate_series(
    make_date(t.period_year, t.period_month, 1),
    (make_date(t.period_year, t.period_month, 1) + INTERVAL '1 month - 1 day')::date,
    INTERVAL '1 day'
) AS d(work_date)
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (SELECT 1 FROM timesheet_day td WHERE td.timesheet_id = t.id AND td.work_date = d.work_date);
