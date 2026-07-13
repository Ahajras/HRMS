-- Return T (late/short worked) to normal paid time-type behavior:
-- pay worked hours only. Any deduction/penalty should be configured separately
-- if needed later.

UPDATE time_type_payroll_rule r
SET action = 'PAY',
    percent = 100.00,
    basis = 'HOURS',
    affects_overtime = FALSE,
    process_separately = FALSE,
    threshold_days = 0,
    threshold_scope = 'NONE',
    year_basis = 'CALENDAR',
    remarks = 'Initialized late rule: pay worked hours.',
    updated_at = now(),
    updated_by = 'V73__late_rules_pay_worked_hours'
FROM time_type tt
WHERE r.time_type_id = tt.id
  AND tt.code = 'T';
