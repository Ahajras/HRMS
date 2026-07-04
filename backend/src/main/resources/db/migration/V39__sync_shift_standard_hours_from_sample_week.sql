-- Keep shift.standard_hours aligned with the sample week used to generate timesheets.
-- The payroll engine reads shift.standard_hours as the employee's shift hours.
UPDATE shift s
SET standard_hours = x.max_normal_hours
FROM (
    SELECT shift_id, MAX(normal_hours) AS max_normal_hours
    FROM shift_day
    WHERE normal_hours > 0
    GROUP BY shift_id
) x
WHERE s.id = x.shift_id;
