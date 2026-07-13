-- T (late/short worked) is a shortage effect. Every active payroll component
-- gets a rule so payroll setup is complete; the action is DEDUCT and quantity
-- is SHORTAGE (planned hours - worked hours). For ACTUAL_WORKED payroll rules,
-- the engine treats this as already unpaid and avoids a second deduction.

INSERT INTO time_type_payroll_rule (
    company_id, time_type_id, payroll_component_id, action, percent, basis,
    affects_overtime, process_separately, sort_order, remarks,
    threshold_days, threshold_scope, year_basis, created_by
)
SELECT tt.company_id,
       tt.id,
       pc.id,
       'DEDUCT',
       100.00,
       'SHORTAGE',
       FALSE,
       FALSE,
       pc.priority,
       'Initialized late rule: deduct planned-vs-worked shortage hours.',
       0,
       'NONE',
       'CALENDAR',
       'V71__late_rules_deduct_shortage_all_components'
FROM time_type tt
JOIN payroll_component pc ON pc.company_id = tt.company_id
WHERE tt.code = 'T'
  AND pc.status = 'ACTIVE'
ON CONFLICT (time_type_id, payroll_component_id) DO UPDATE
SET action = 'DEDUCT',
    percent = 100.00,
    basis = 'SHORTAGE',
    affects_overtime = FALSE,
    process_separately = FALSE,
    sort_order = EXCLUDED.sort_order,
    remarks = 'Initialized late rule: deduct planned-vs-worked shortage hours.',
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    updated_at = now(),
    updated_by = 'V71__late_rules_deduct_shortage_all_components';
