-- =====================================================================
-- V63  Day Zero — backfill new_time_type_id / timesheet_day_id for
--   adjustments created BEFORE those structured columns existed. Without
--   this, usage counting (Time Usage tab, payroll thresholds) silently
--   ignores every Day Zero correction made before that point — exactly
--   what was seen for employee 77677 (2 extra sick days, 2 extra unpaid
--   days from Day Zero, invisible to Time Usage).
--
--   timesheet_day_id is recovered by matching employee + work_date (both
--   already stored on the adjustment).
--   new_time_type_id is recovered by parsing the time-type CODE out of
--   the reason text, which has always followed the same
--   "... -> CODE - Name)" pattern since Day Zero was first built.
-- =====================================================================

UPDATE payroll_adjustment pa
SET timesheet_day_id = td.id
FROM timesheet_day td
JOIN timesheet t ON t.id = td.timesheet_id
WHERE t.employee_id = pa.employee_id
  AND td.work_date = pa.work_date
  AND pa.timesheet_day_id IS NULL;

UPDATE payroll_adjustment pa
SET new_time_type_id = tt.id
FROM time_type tt
WHERE pa.new_time_type_id IS NULL
  AND tt.company_id = pa.company_id
  AND pa.reason ~ '-> [A-Za-z0-9]+ - '
  AND tt.code = substring(pa.reason from '-> ([A-Za-z0-9]+) - ');
