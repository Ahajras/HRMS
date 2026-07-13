-- T (late/short worked) must have payroll rules for every active component so
-- payroll calculation can evaluate employees who have allowances. Basic salary
-- deducts shortage; other components are paid for worked hours by default.

INSERT INTO time_type_payroll_rule (
    company_id, time_type_id, payroll_component_id, action, percent, basis,
    affects_overtime, process_separately, sort_order, remarks,
    threshold_days, threshold_scope, year_basis, created_by
)
SELECT tt.company_id,
       tt.id,
       pc.id,
       CASE WHEN pc.category = 'SALARY'
              AND (pc.code IN ('00', '01', 'LEG00')
                   OR upper(pc.name) LIKE '%BASIC%'
                   OR upper(pc.name) LIKE '%BASE%')
            THEN 'DEDUCT' ELSE 'PAY' END,
       100.00,
       CASE WHEN pc.category = 'SALARY'
              AND (pc.code IN ('00', '01', 'LEG00')
                   OR upper(pc.name) LIKE '%BASIC%'
                   OR upper(pc.name) LIKE '%BASE%')
            THEN 'SHORTAGE' ELSE 'HOURS' END,
       FALSE,
       FALSE,
       CASE WHEN pc.category = 'SALARY'
              AND (pc.code IN ('00', '01', 'LEG00')
                   OR upper(pc.name) LIKE '%BASIC%'
                   OR upper(pc.name) LIKE '%BASE%')
            THEN 100 ELSE pc.priority END,
       CASE WHEN pc.category = 'SALARY'
              AND (pc.code IN ('00', '01', 'LEG00')
                   OR upper(pc.name) LIKE '%BASIC%'
                   OR upper(pc.name) LIKE '%BASE%')
            THEN 'Initialized late rule: deduct planned-vs-worked shortage from basic salary.'
            ELSE 'Initialized late rule: pay this component for the worked hours on the late day.' END,
       0,
       'NONE',
       'CALENDAR',
       'V70__restore_late_default_component_rules'
FROM time_type tt
JOIN payroll_component pc ON pc.company_id = tt.company_id
WHERE tt.code = 'T'
  AND pc.status = 'ACTIVE'
ON CONFLICT (time_type_id, payroll_component_id) DO UPDATE
SET action = EXCLUDED.action,
    percent = EXCLUDED.percent,
    basis = EXCLUDED.basis,
    affects_overtime = EXCLUDED.affects_overtime,
    process_separately = EXCLUDED.process_separately,
    sort_order = EXCLUDED.sort_order,
    remarks = EXCLUDED.remarks,
    threshold_days = EXCLUDED.threshold_days,
    threshold_scope = EXCLUDED.threshold_scope,
    year_basis = EXCLUDED.year_basis,
    updated_at = now(),
    updated_by = 'V70__restore_late_default_component_rules';
