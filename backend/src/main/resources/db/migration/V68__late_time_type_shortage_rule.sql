-- Add a configurable late/short-worked time type. Payroll effects are driven
-- by time_type_payroll_rule using SHORTAGE = planned hours - worked hours.

INSERT INTO time_type (
    company_id, code, name, category, paid, counts_as_worked, affects_leave,
    factor, sort_order, status, created_by
)
SELECT c.company_id, 'T', 'Late / short worked', 'REGULAR', TRUE, TRUE, FALSE,
       1.000, 113, 'ACTIVE', 'V68__late_time_type_shortage_rule'
FROM (SELECT DISTINCT company_id FROM time_type) c
ON CONFLICT (company_id, code) DO UPDATE
SET name = EXCLUDED.name,
    category = EXCLUDED.category,
    paid = EXCLUDED.paid,
    counts_as_worked = EXCLUDED.counts_as_worked,
    affects_leave = EXCLUDED.affects_leave,
    factor = EXCLUDED.factor,
    sort_order = EXCLUDED.sort_order,
    status = EXCLUDED.status,
    updated_at = now(),
    updated_by = 'V68__late_time_type_shortage_rule';

INSERT INTO time_type_payroll_rule (
    company_id, time_type_id, payroll_component_id, action, percent, basis,
    affects_overtime, process_separately, sort_order, remarks,
    threshold_days, threshold_scope, year_basis, created_by
)
SELECT tt.company_id, tt.id, pc.id, 'DEDUCT', 100.00, 'SHORTAGE',
       FALSE, FALSE, 100,
       'Late/short worked: deduct only planned-vs-worked shortage from basic salary.',
       0, 'NONE', 'CALENDAR', 'V68__late_time_type_shortage_rule'
FROM time_type tt
JOIN payroll_component pc ON pc.company_id = tt.company_id
WHERE tt.code = 'T'
  AND pc.status = 'ACTIVE'
  AND (
      pc.code IN ('01', 'LEG00')
      OR upper(pc.name) IN ('BASE SALARY', 'BASIC SALARY')
  )
ON CONFLICT (time_type_id, payroll_component_id) DO UPDATE
SET action = 'DEDUCT',
    percent = 100.00,
    basis = 'SHORTAGE',
    affects_overtime = FALSE,
    process_separately = FALSE,
    sort_order = 100,
    remarks = 'Late/short worked: deduct only planned-vs-worked shortage from basic salary.',
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    updated_at = now(),
    updated_by = 'V68__late_time_type_shortage_rule';

UPDATE timesheet_day td
SET time_type_id = tt_late.id,
    updated_at = now(),
    updated_by = 'V68__late_time_type_shortage_rule'
FROM timesheet ts, time_type tt_normal, time_type tt_late
WHERE td.timesheet_id = ts.id
  AND td.time_type_id = tt_normal.id
  AND tt_normal.company_id = ts.company_id
  AND tt_normal.code = 'N'
  AND tt_late.company_id = ts.company_id
  AND tt_late.code = 'T'
  AND COALESCE(td.planned_hours, 0) > 0
  AND COALESCE(td.worked_hours, 0) > 0
  AND COALESCE(td.worked_hours, 0) < COALESCE(td.planned_hours, 0);
