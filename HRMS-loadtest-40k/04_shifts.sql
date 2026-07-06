-- =====================================================================
-- LOAD TEST DATA GENERATOR — PART 4: Shifts
-- One shift per project, with different weekly-off patterns, so the
-- test also exercises "off day depends on the employee's own shift"
-- logic — not just a hardcoded Friday.
--
--   LT-P1  -> Friday + Saturday off
--   LT-P2  -> Friday only off
--   LT-P3  -> Saturday only off
--   LT-P4  -> Friday + Saturday off
--   LT-P5  -> Friday only off
--
-- Run this AFTER 03_assignment_contract.sql and BEFORE generating
-- timesheets (from the SQL script or from the screen).
-- =====================================================================

-- 4a) One shift per project
INSERT INTO shift (company_id, project_id, code, name, start_time, end_time, break_minutes, standard_hours, weekly_off)
SELECT '00000000-0000-0000-0000-0000000000c1', p.id, 'LT-SHIFT-' || p.code, 'Load Test Shift — ' || p.code,
       TIME '08:00', TIME '17:00', 60, 8.00, v.weekly_off
FROM project p
JOIN (VALUES
    ('LT-P1', 'FRI,SAT'),
    ('LT-P2', 'FRI'),
    ('LT-P3', 'SAT'),
    ('LT-P4', 'FRI,SAT'),
    ('LT-P5', 'FRI')
) AS v(code, weekly_off) ON v.code = p.code
WHERE p.company_id = '00000000-0000-0000-0000-0000000000c1'
  AND NOT EXISTS (SELECT 1 FROM shift s WHERE s.company_id = p.company_id AND s.code = 'LT-SHIFT-' || p.code);

-- 4b) Sample week (shift_day) for each new shift — 8 normal hours on
--     working days, 0 on the days marked as weekly off for that shift.
INSERT INTO shift_day (company_id, shift_id, day_of_week, normal_hours, declared_ot, weekly_off)
SELECT '00000000-0000-0000-0000-0000000000c1', s.id, d.dow,
       CASE WHEN d.dow = ANY(string_to_array(s.weekly_off, ',')) THEN 0 ELSE 8 END,
       0,
       d.dow = ANY(string_to_array(s.weekly_off, ','))
FROM shift s
CROSS JOIN (VALUES ('SAT'),('SUN'),('MON'),('TUE'),('WED'),('THU'),('FRI')) AS d(dow)
WHERE s.code LIKE 'LT-SHIFT-%'
  AND NOT EXISTS (SELECT 1 FROM shift_day sd WHERE sd.shift_id = s.id AND sd.day_of_week = d.dow);

-- 4c) Assign each load-test employee to their own project's shift
INSERT INTO employee_shift (company_id, employee_id, shift_id, effective_from, status)
SELECT '00000000-0000-0000-0000-0000000000c1', e.id, s.id, e.hire_date, 'ACTIVE'
FROM employee e
JOIN assignment a ON a.employee_id = e.id
JOIN project p ON p.id = a.project_id
JOIN shift s ON s.code = 'LT-SHIFT-' || p.code
WHERE e.employee_number LIKE 'LT-%'
  AND NOT EXISTS (SELECT 1 FROM employee_shift es WHERE es.employee_id = e.id AND es.status = 'ACTIVE');
