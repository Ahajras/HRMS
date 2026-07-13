-- Keep late/short-worked payroll effects explicit: T deducts shortage from
-- basic salary only. Remove initialized PAY rows that may have been created
-- for allowances after V68.

DELETE FROM time_type_payroll_rule r
USING time_type tt, payroll_component pc
WHERE r.time_type_id = tt.id
  AND r.payroll_component_id = pc.id
  AND tt.code = 'T'
  AND NOT (
      pc.category = 'SALARY'
      AND (
          pc.code IN ('00', '01', 'LEG00')
          OR upper(pc.name) LIKE '%BASIC%'
          OR upper(pc.name) LIKE '%BASE%'
      )
  );

INSERT INTO time_type_payroll_rule (
    company_id, time_type_id, payroll_component_id, action, percent, basis,
    affects_overtime, process_separately, sort_order, remarks,
    threshold_days, threshold_scope, year_basis, created_by
)
SELECT tt.company_id, tt.id, pc.id, 'DEDUCT', 100.00, 'SHORTAGE',
       FALSE, FALSE, 100,
       'Late/short worked: deduct only planned-vs-worked shortage from basic salary.',
       0, 'NONE', 'CALENDAR', 'V69__clean_late_time_type_rules'
FROM time_type tt
JOIN payroll_component pc ON pc.company_id = tt.company_id
WHERE tt.code = 'T'
  AND pc.status = 'ACTIVE'
  AND pc.category = 'SALARY'
  AND (
      pc.code IN ('00', '01', 'LEG00')
      OR upper(pc.name) LIKE '%BASIC%'
      OR upper(pc.name) LIKE '%BASE%'
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
    updated_by = 'V69__clean_late_time_type_rules';
